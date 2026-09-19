package com.luggagestorage.store.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.entity.PaymentStatus;
import com.luggagestorage.payment.repository.PaymentRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        Member member = getCurrentMember();
        validateDateRange(request.startDate(), request.endDate());

        Store store = new Store(
            member,
            request.name(),
            request.description(),
            request.address(),
            request.category(),
            request.luggageCount(),
            request.startDate(),
            request.endDate(),
            calculateTotalPrice(request.category(), request.luggageCount(), request.startDate(), request.endDate())
        );
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
        validateDateRange(request.startDate(), request.endDate());

        store.update(
            request.name(),
            request.description(),
            request.address(),
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

    @Transactional
    public void deleteStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);

        paymentRepository.findByStore(store).ifPresent(paymentRepository::delete);
        storeImageRepository.deleteByStore(store);
        storeRepository.delete(store);
    }

    @Transactional
    public StoreResponse pickUpStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.PENDING);
        requirePaid(store);

        store.pickUp();
        return toResponse(store, imageUrlsOf(store));
    }

    @Transactional
    public StoreResponse beginStorage(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.PICKED_UP);

        store.beginStorage();
        return toResponse(store, imageUrlsOf(store));
    }

    @Transactional
    public StoreResponse completeStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.IN_USE);

        store.complete();
        return toResponse(store, imageUrlsOf(store));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_STORE_TIME);
        }
    }

    private int calculateTotalPrice(StoreCategory category, Integer luggageCount, LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return (int) (category.getDailyRate() * luggageCount * days);
    }

    private void requirePaid(Store store) {
        boolean paid = paymentRepository.findByStore(store)
            .map(payment -> payment.getStatus() == PaymentStatus.DONE)
            .orElse(false);
        if (!paid) {
            throw new BusinessException(ErrorCode.PAYMENT_REQUIRED);
        }
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

    private StoreResponse toResponse(Store store, List<String> imageUrls) {
        PaymentStatus paymentStatus = paymentRepository.findByStore(store)
            .map(Payment::getStatus)
            .orElse(null);
        return StoreResponse.of(store, imageUrls, paymentStatus);
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
