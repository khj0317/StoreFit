package com.luggagestorage.owner.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.owner.dto.OwnerDashboardResponse;
import com.luggagestorage.owner.dto.OwnerReservationResponse;
import com.luggagestorage.owner.dto.OwnerReservationResponse.NextAction;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.place.dto.BranchApplicationResponse;
import com.luggagestorage.place.dto.BranchApplyRequest;
import com.luggagestorage.place.dto.PlaceRequest;
import com.luggagestorage.place.entity.BranchApplication;
import com.luggagestorage.place.repository.BranchApplicationRepository;
import com.luggagestorage.store.service.ReservationNotifier;
import com.luggagestorage.place.dto.PlaceResponse;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import com.luggagestorage.place.service.CapacityCalculator;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 보관소 운영자 기능. 모든 메서드는 SecurityConfig에서 OWNER 역할로 막혀 있고,
 * 여기서는 "자기 보관소의 예약인지"를 한 번 더 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OwnerService {

    /** 수용량을 줄일 때, 앞으로 이 기간 안의 예약까지 확인한다 (예약은 최대 60일 + 여유) */
    private static final int CAPACITY_CHECK_DAYS = 120;

    private final StoragePlaceRepository storagePlaceRepository;
    private final StoreRepository storeRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final BranchApplicationRepository applicationRepository;
    private final ReservationNotifier reservationNotifier;
    private final Clock clock;

    // ───────── 보관소 관리 ─────────

    public List<PlaceResponse> getMyPlaces() {
        Member owner = getCurrentMember();
        List<StoragePlace> places = storagePlaceRepository.findByOwnerOrderByIdAsc(owner);
        Map<Long, Integer> occupied = occupiedToday(places);
        return places.stream()
            .map(place -> PlaceResponse.of(place, place.getCapacity() - occupied.getOrDefault(place.getId(), 0)))
            .toList();
    }

    /** 아직 운영자가 없는 정식 지점 목록 */
    public List<PlaceResponse> getAvailableBranches() {
        return storagePlaceRepository.findByOwnerIsNullOrderByIdAsc().stream()
            .map(place -> PlaceResponse.of(place, null))
            .toList();
    }

    /**
     * 정식 지점 운영 신청. 바로 맡지 않고 본사 관리자가 승인해야 운영자가 된다 (AdminService.approve).
     * 같은 지점에 여러 운영자가 신청할 수 있고, 승인되는 한 명 외에는 자동으로 반려된다.
     */
    @Transactional
    public BranchApplicationResponse applyForBranch(Long placeId, BranchApplyRequest request) {
        Member owner = getCurrentMember();
        StoragePlace place = storagePlaceRepository.findById(placeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        if (place.isOperating()) {
            throw new BusinessException(ErrorCode.BRANCH_ALREADY_TAKEN);
        }
        if (applicationRepository.existsByPlaceAndApplicantAndStatus(place, owner, BranchApplication.Status.PENDING)) {
            throw new BusinessException(ErrorCode.BRANCH_APPLICATION_DUPLICATE);
        }

        BranchApplication application = applicationRepository.save(new BranchApplication(
            place, owner, request.capacity(), blankToNull(request.description()), blankToNull(request.message())));
        return BranchApplicationResponse.from(application);
    }

    public List<BranchApplicationResponse> getMyApplications() {
        return applicationRepository.findByApplicant(getCurrentMember()).stream()
            .map(BranchApplicationResponse::from)
            .toList();
    }

    @Transactional
    public PlaceResponse updatePlace(Long placeId, PlaceRequest request) {
        Member owner = getCurrentMember();
        // 수용량을 바꾸는 동안 새 예약이 끼어들지 않게 예약과 같은 락을 잡는다
        StoragePlace place = storagePlaceRepository.findByIdForUpdate(placeId)
            .filter(found -> found.isOwnedBy(owner.getId()))
            .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        LocalDate today = LocalDate.now(clock);
        LocalDate until = today.plusDays(CAPACITY_CHECK_DAYS);
        List<Store> upcoming = storeRepository.findOverlapping(List.of(place), StoreStatus.ACTIVE, today, until);
        if (CapacityCalculator.peakOccupancy(upcoming, today, until) > request.capacity()) {
            throw new BusinessException(ErrorCode.PLACE_CAPACITY_TOO_SMALL);
        }

        place.updateOperation(request.capacity(), blankToNull(request.description()));
        int occupied = CapacityCalculator.peakOccupancy(upcoming, today, today);
        return PlaceResponse.of(place, place.getCapacity() - occupied);
    }

    // ───────── QR 체크인 / 체크아웃 ─────────

    /** QR을 스캔하면 먼저 어떤 예약인지 보여주고, 운영자가 확인 버튼을 누르면 처리한다 */
    public OwnerReservationResponse lookup(String code) {
        Store store = storeRepository.findByCheckInCode(normalize(code))
            .orElseThrow(() -> new BusinessException(ErrorCode.CHECK_IN_CODE_NOT_FOUND));
        requirePlaceOwner(store);
        return toResponse(store, isPaid(store));
    }

    @Transactional
    public OwnerReservationResponse checkIn(String code) {
        Store store = lockByCode(code);
        requirePlaceOwner(store);
        if (store.getStatus() != StoreStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STORE_STATUS);
        }
        if (!isPaid(store)) {
            throw new BusinessException(ErrorCode.PAYMENT_REQUIRED);
        }
        // 시작일 당일에만 받는다. 시작일이 지나도록 오지 않으면 노쇼로 처리돼 자리가 다른 손님에게 돌아간다
        if (!LocalDate.now(clock).equals(store.getStartDate())) {
            throw new BusinessException(ErrorCode.CHECK_IN_DATE_INVALID);
        }

        store.checkIn(LocalDateTime.now(clock));
        reservationNotifier.checkedIn(store);
        return toResponse(store, true);
    }

    @Transactional
    public OwnerReservationResponse checkOut(String code) {
        Store store = lockByCode(code);
        requirePlaceOwner(store);
        if (store.getStatus() != StoreStatus.IN_USE) {
            throw new BusinessException(ErrorCode.INVALID_STORE_STATUS);
        }

        // 연체료는 체크아웃 직전 기준으로 보여주고 현장에서 받는다
        OwnerReservationResponse beforeCheckOut = toResponse(store, true);
        store.checkOut(LocalDateTime.now(clock));
        reservationNotifier.checkedOut(store);
        OwnerReservationResponse after = toResponse(store, true);
        return after.withOverdue(beforeCheckOut.overdueDays(), beforeCheckOut.overdueFee());
    }

    // ───────── 예약 목록 / 대시보드 ─────────

    /** 내 보관소들의 진행 중 예약 (결제 전 예약도 자리를 차지하므로 함께 보여준다) */
    public List<OwnerReservationResponse> getReservations() {
        List<StoragePlace> places = storagePlaceRepository.findByOwnerOrderByIdAsc(getCurrentMember());
        if (places.isEmpty()) {
            return List.of();
        }
        List<Store> stores = storeRepository.findByPlacesAndStatuses(places, StoreStatus.ACTIVE);
        Set<Long> paidIds = paidStoreIds(stores);
        return stores.stream()
            .map(store -> toResponse(store, paidIds.contains(store.getId())))
            .toList();
    }

    public OwnerDashboardResponse getDashboard() {
        LocalDate today = LocalDate.now(clock);
        List<StoragePlace> places = storagePlaceRepository.findByOwnerOrderByIdAsc(getCurrentMember());
        if (places.isEmpty()) {
            return new OwnerDashboardResponse(today, List.of(), List.of(), 0, 0, List.of());
        }

        List<Store> active = storeRepository.findByPlacesAndStatuses(places, StoreStatus.ACTIVE);
        Set<Long> paidIds = paidStoreIds(active);

        List<OwnerReservationResponse> arrivals = active.stream()
            .filter(store -> store.getStatus() == StoreStatus.PENDING && paidIds.contains(store.getId()))
            .filter(store -> today.equals(store.getStartDate()))
            .map(store -> toResponse(store, true))
            .toList();
        List<OwnerReservationResponse> departures = active.stream()
            .filter(store -> store.getStatus() == StoreStatus.IN_USE && !store.getEndDate().isAfter(today))
            .map(store -> toResponse(store, true))
            .toList();
        int stored = active.stream()
            .filter(store -> store.getStatus() == StoreStatus.IN_USE)
            .mapToInt(Store::getLuggageCount)
            .sum();

        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        long revenue = paymentRepository.sumNetRevenue(places, monthStart, monthStart.plusMonths(1));

        Map<Long, Integer> occupied = occupiedToday(places);
        List<OwnerDashboardResponse.PlaceOccupancy> occupancies = places.stream()
            .map(place -> new OwnerDashboardResponse.PlaceOccupancy(
                place.getId(), place.getName(), place.getAddress(), place.getCapacity(),
                occupied.getOrDefault(place.getId(), 0)))
            .toList();

        return new OwnerDashboardResponse(today, arrivals, departures, stored, revenue, occupancies);
    }

    // ───────── helpers ─────────

    private Map<Long, Integer> occupiedToday(List<StoragePlace> places) {
        if (places.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now(clock);
        return storeRepository.findOverlapping(places, StoreStatus.ACTIVE, today, today).stream()
            .collect(Collectors.groupingBy(store -> store.getPlace().getId(), Collectors.summingInt(Store::getLuggageCount)));
    }

    private Set<Long> paidStoreIds(List<Store> stores) {
        if (stores.isEmpty()) {
            return Set.of();
        }
        return paymentRepository.findByStoreIn(stores).stream()
            .filter(Payment::isPaid)
            .map(payment -> payment.getStore().getId())
            .collect(Collectors.toSet());
    }

    private boolean isPaid(Store store) {
        return paymentRepository.findByStore(store).map(Payment::isPaid).orElse(false);
    }

    private OwnerReservationResponse toResponse(Store store, boolean paid) {
        return OwnerReservationResponse.of(store, paid, nextAction(store, paid), LocalDate.now(clock));
    }

    private NextAction nextAction(Store store, boolean paid) {
        LocalDate today = LocalDate.now(clock);
        return switch (store.getStatus()) {
            case PENDING -> paid && today.equals(store.getStartDate()) ? NextAction.CHECK_IN : NextAction.NONE;
            case IN_USE -> NextAction.CHECK_OUT;
            default -> NextAction.NONE;
        };
    }

    private Store lockByCode(String code) {
        return storeRepository.findByCheckInCodeForUpdate(normalize(code))
            .orElseThrow(() -> new BusinessException(ErrorCode.CHECK_IN_CODE_NOT_FOUND));
    }

    /** 다른 보관소의 코드는 존재 여부도 알려주지 않는다 */
    private void requirePlaceOwner(Store store) {
        if (!store.getPlace().isOwnedBy(getCurrentMember().getId())) {
            throw new BusinessException(ErrorCode.CHECK_IN_CODE_NOT_FOUND);
        }
    }

    /** 직접 입력할 때 소문자·공백·하이픈이 섞여도 같은 코드로 본다 */
    private static String normalize(String code) {
        return code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Member getCurrentMember() {
        return memberRepository.findByUsername(SecurityUtil.getCurrentUsername())
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
