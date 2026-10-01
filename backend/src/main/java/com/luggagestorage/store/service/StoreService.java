package com.luggagestorage.store.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.client.TossPaymentsClient;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.payment.service.RefundPolicy;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import com.luggagestorage.place.service.CapacityCalculator;
import com.luggagestorage.store.dto.RefundPreviewResponse;
import com.luggagestorage.store.dto.StoreCreateRequest;
import com.luggagestorage.store.dto.StoreResponse;
import com.luggagestorage.store.dto.StoreUpdateRequest;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
import com.luggagestorage.store.entity.StoreImage;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreImageRepository;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    static final int MAX_STORAGE_DAYS = 60;

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final ReservationNotifier reservationNotifier;
    private final Clock clock;

    @Value("${app.payment-timeout-minutes:30}")
    private long paymentTimeoutMinutes = 30;

    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        Member member = getCurrentMember();
        validateDateRange(request.startDate(), request.endDate());

        StoragePlace place = lockPlace(request.placeId());
        requireCapacity(place, request.startDate(), request.endDate(), request.luggageCount(), null);

        Store store = new Store(
            member,
            place,
            request.name(),
            request.description(),
            request.category(),
            request.luggageCount(),
            request.startDate(),
            request.endDate(),
            calculateTotalPrice(request.category(), request.luggageCount(), request.startDate(), request.endDate())
        );
        // 결제하지 않은 예약이 자리를 계속 차지하지 않도록 결제 마감을 둔다 (지나면 StoreLifecycleService가 취소)
        store.setPaymentDeadline(LocalDateTime.now(clock).plusMinutes(paymentTimeoutMinutes));
        storeRepository.save(store);

        List<String> imageUrls = saveImages(store, request.imageUrls());
        return toResponse(store, imageUrls);
    }

    public List<StoreResponse> getMyStores() {
        Member member = getCurrentMember();
        return storeRepository.findByMemberOrderByCreatedAtDesc(member).stream()
            .map(store -> toResponse(store, imageUrlsOf(store)))
            .toList();
    }

    public StoreResponse getStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        return toResponse(store, imageUrlsOf(store));
    }

    @Transactional
    public StoreResponse updateStore(Long storeId, StoreUpdateRequest request) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.PENDING);
        // 결제한 금액과 예약 내용이 달라지지 않도록, 결제한 예약은 취소 후 다시 예약하게 한다
        Payment payment = findPayment(store);
        if (payment != null && payment.isPaid()) {
            throw new BusinessException(ErrorCode.ALREADY_PAID);
        }
        validateDateRange(request.startDate(), request.endDate());

        StoragePlace place = lockPlace(request.placeId());
        requireCapacity(place, request.startDate(), request.endDate(), request.luggageCount(), store.getId());

        store.update(
            place,
            request.name(),
            request.description(),
            request.category(),
            request.luggageCount(),
            request.startDate(),
            request.endDate(),
            calculateTotalPrice(request.category(), request.luggageCount(), request.startDate(), request.endDate())
        );

        List<String> imageUrls;
        if (request.imageUrls() != null) {
            storeImageRepository.deleteByStore(store);
            imageUrls = saveImages(store, request.imageUrls());
        } else {
            imageUrls = imageUrlsOf(store);
        }

        return toResponse(store, imageUrls);
    }

    /** 결제 전 예약은 기록 없이 지운다. 결제한 예약은 환불이 필요하므로 cancelStore로 처리한다 */
    @Transactional
    public void deleteStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.PENDING);

        Payment payment = findPayment(store);
        if (payment != null && payment.isPaid()) {
            throw new BusinessException(ErrorCode.PAID_RESERVATION_REQUIRES_CANCEL);
        }

        if (payment != null) {
            paymentRepository.delete(payment);
        }
        storeImageRepository.deleteByStore(store);
        storeRepository.delete(store);
    }

    public RefundPreviewResponse previewRefund(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        Payment payment = requirePaidPending(store);

        LocalDate today = LocalDate.now(clock);
        return new RefundPreviewResponse(
            payment.getAmount(),
            RefundPolicy.refundAmount(payment.getAmount(), today, store.getStartDate()),
            RefundPolicy.refundRate(today, store.getStartDate()),
            RefundPolicy.DESCRIPTION
        );
    }

    /** 결제한 예약 취소: 규칙에 따라 Toss로 (부분) 환불하고 예약은 취소 상태로 남긴다 */
    @Transactional
    public StoreResponse cancelStore(Long storeId) {
        // 취소 버튼을 연달아 누르거나 두 기기에서 동시에 취소해도 환불은 한 번만 되도록 행을 잠그고 상태를 본다
        Store store = storeRepository.findByIdForUpdate(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
        requireOwner(store);
        Payment payment = requirePaidPending(store);

        int refundAmount = RefundPolicy.refundAmount(payment.getAmount(), LocalDate.now(clock), store.getStartDate());
        if (refundAmount > 0) {
            tossPaymentsClient.cancel(payment.getPaymentKey(), "고객 요청 취소", refundAmount, "cancel-" + payment.getOrderId());
        }
        payment.cancel(refundAmount, LocalDateTime.now(clock));
        store.cancel();
        reservationNotifier.refunded(store, refundAmount);

        return toResponse(store, imageUrlsOf(store));
    }

    private Payment requirePaidPending(Store store) {
        requireStatus(store, StoreStatus.PENDING);
        Payment payment = findPayment(store);
        if (payment == null || !payment.isPaid()) {
            throw new BusinessException(ErrorCode.INVALID_STORE_STATUS);
        }
        return payment;
    }

    private StoragePlace lockPlace(Long placeId) {
        // 운영자가 없는 지점은 체크인해줄 사람이 없으므로 예약을 받지 않는다
        return storagePlaceRepository.findByIdForUpdate(placeId)
            .filter(StoragePlace::isOperating)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }

    /**
     * 보관소 행 락을 잡은 상태에서 호출해야 한다. 기간 중 가장 붐비는 날에 이 짐까지 더해도
     * 수용량을 넘지 않는지 본다. 예약 수정이면 자기 자신의 기존 예약은 빼고 계산한다.
     */
    private void requireCapacity(StoragePlace place, LocalDate startDate, LocalDate endDate, int luggageCount, Long excludeStoreId) {
        List<Store> overlapping = storeRepository.findOverlapping(List.of(place), StoreStatus.ACTIVE, startDate, endDate)
            .stream()
            .filter(store -> !store.getId().equals(excludeStoreId))
            .toList();
        int peak = CapacityCalculator.peakOccupancy(overlapping, startDate, endDate);
        if (peak + luggageCount > place.getCapacity()) {
            throw new BusinessException(ErrorCode.PLACE_FULL);
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate) || startDate.isBefore(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.INVALID_STORE_TIME);
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_STORAGE_DAYS) {
            throw new BusinessException(ErrorCode.STORAGE_PERIOD_TOO_LONG);
        }
    }

    private int calculateTotalPrice(StoreCategory category, Integer luggageCount, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return (int) (category.getDailyRate() * luggageCount * days);
    }

    private List<String> saveImages(Store store, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }

        List<StoreImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(new StoreImage(store, imageUrls.get(i), i));
        }
        storeImageRepository.saveAll(images);
        return imageUrls;
    }

    private List<String> imageUrlsOf(Store store) {
        return storeImageRepository.findByStoreOrderBySortOrderAsc(store).stream()
            .map(StoreImage::getImageUrl)
            .toList();
    }

    private Payment findPayment(Store store) {
        return paymentRepository.findByStore(store).orElse(null);
    }

    private StoreResponse toResponse(Store store, List<String> imageUrls) {
        return StoreResponse.of(store, imageUrls, findPayment(store), LocalDate.now(clock));
    }

    private void requireOwner(Store store) {
        Member current = getCurrentMember();
        if (!store.isOwnedBy(current.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void requireStatus(Store store, StoreStatus expected) {
        if (store.getStatus() != expected) {
            throw new BusinessException(ErrorCode.INVALID_STORE_STATUS);
        }
    }

    private Store getStoreOrThrow(Long storeId) {
        return storeRepository.findById(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
    }

    private Member getCurrentMember() {
        String username = SecurityUtil.getCurrentUsername();
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
