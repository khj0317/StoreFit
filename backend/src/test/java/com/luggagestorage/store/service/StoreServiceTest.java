package com.luggagestorage.store.service;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.client.TossPaymentsClient;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.entity.PaymentStatus;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import com.luggagestorage.store.dto.StoreCreateRequest;
import com.luggagestorage.store.dto.StoreResponse;
import com.luggagestorage.store.dto.StoreUpdateRequest;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreImageRepository;
import com.luggagestorage.store.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    /** 날짜 규칙(환불, 시작일 검증)을 검증하려고 "오늘"을 고정한다 */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private StoragePlaceRepository storagePlaceRepository;
    @Mock
    private TossPaymentsClient tossPaymentsClient;
    @Mock
    private ReservationNotifier reservationNotifier;

    private StoreService storeService;
    private StoragePlace place;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(TODAY.atTime(10, 0).atZone(KST).toInstant(), KST);
        storeService = new StoreService(storeRepository, storeImageRepository, paymentRepository, memberRepository,
            storagePlaceRepository, tossPaymentsClient, reservationNotifier, clock);

        Member boss = Member.createOwner("boss", "encoded", "사장님", null, null);
        ReflectionTestUtils.setField(boss, "id", 99L);
        place = new StoragePlace(boss, "보관소", "주소", null, 5);
        ReflectionTestUtils.setField(place, "id", 100L);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Member loginAs(Long id, String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(username, null));
        Member member = Member.createUser(username, "encoded", "이름", null, null);
        ReflectionTestUtils.setField(member, "id", id);
        when(memberRepository.findByUsername(username)).thenReturn(Optional.of(member));
        return member;
    }

    private Store storeWithId(Long id, Member owner, LocalDate start, LocalDate end, int luggageCount) {
        Store store = new Store(owner, place, "짐", null, StoreCategory.LIGHT, luggageCount, start, end, 3000);
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

    private Payment paidPayment(Store store, int amount) {
        Payment payment = new Payment(store, "order-" + store.getId(), amount);
        payment.approve("toss-key", "카드", LocalDateTime.now());
        return payment;
    }

    private StoreCreateRequest createRequest(int luggageCount, LocalDate start, LocalDate end) {
        return new StoreCreateRequest(100L, "짐", null, List.of(), StoreCategory.LIGHT, luggageCount, start, end);
    }

    // ───────── 예약 만들기 ─────────

    @Test
    void createStore_calculatesTotalPriceFromCategoryLuggageCountAndDays() {
        loginAs(1L, "user1");
        when(storagePlaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(place));
        when(storeRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of());

        StoreResponse response = storeService.createStore(createRequest(2, TODAY, TODAY.plusDays(2)));

        // LIGHT dailyRate 3000 * 2 luggage * 3 days = 18000
        assertThat(response.totalPrice()).isEqualTo(18_000);
        assertThat(response.placeName()).isEqualTo("보관소");
    }

    @Test
    void createStore_setsPaymentDeadline30MinutesLater() {
        loginAs(1L, "user1");
        when(storagePlaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(place));
        when(storeRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of());

        StoreResponse response = storeService.createStore(createRequest(1, TODAY, TODAY));

        assertThat(response.paymentDeadline()).isEqualTo(TODAY.atTime(10, 30));
    }

    @Test
    void createStore_atBranchWithoutOwner_isRejected() {
        loginAs(1L, "user1");
        StoragePlace unclaimed = StoragePlace.officialBranch("NEW", "새 지점", "주소", null, 10);
        when(storagePlaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(unclaimed));

        assertThatThrownBy(() -> storeService.createStore(createRequest(1, TODAY, TODAY)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    void createStore_locksPlaceBeforeCheckingCapacity() {
        loginAs(1L, "user1");
        when(storagePlaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(place));
        when(storeRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of());

        storeService.createStore(createRequest(1, TODAY, TODAY));

        // 일반 findById가 아니라 SELECT ... FOR UPDATE로 보관소를 가져와야 동시 예약이 한 줄로 처리된다
        verify(storagePlaceRepository).findByIdForUpdate(100L);
        verify(storagePlaceRepository, never()).findById(any());
    }

    @Test
    void createStore_overCapacityOnBusiestDay_throwsPlaceFull() {
        Member user = loginAs(1L, "user1");
        when(storagePlaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(place));
        // 수용량 5. 10/2에만 4개가 이미 맡겨져 있다 → 10/1~10/3에 2개를 맡기면 10/2에 6개가 된다
        Store existing = storeWithId(10L, user, TODAY.plusDays(1), TODAY.plusDays(1), 4);
        when(storeRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> storeService.createStore(createRequest(2, TODAY, TODAY.plusDays(2))))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PLACE_FULL);
        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStore_fillsExactlyToCapacity_succeeds() {
        Member user = loginAs(1L, "user1");
        when(storagePlaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(place));
        Store existing = storeWithId(10L, user, TODAY, TODAY, 3);
        when(storeRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of(existing));

        StoreResponse response = storeService.createStore(createRequest(2, TODAY, TODAY));

        assertThat(response.luggageCount()).isEqualTo(2);
    }

    @Test
    void createStore_endBeforeStart_throws() {
        loginAs(1L, "user1");

        assertThatThrownBy(() -> storeService.createStore(createRequest(1, TODAY.plusDays(3), TODAY)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STORE_TIME);
    }

    @Test
    void createStore_startInPast_throws() {
        loginAs(1L, "user1");

        assertThatThrownBy(() -> storeService.createStore(createRequest(1, TODAY.minusDays(1), TODAY)))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STORE_TIME);
    }

    @Test
    void createStore_longerThan60Days_throws() {
        loginAs(1L, "user1");

        assertThatThrownBy(() -> storeService.createStore(createRequest(1, TODAY, TODAY.plusDays(60))))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.STORAGE_PERIOD_TOO_LONG);
    }

    // ───────── 수정 ─────────

    @Test
    void updateStore_notOwner_throwsAccessDenied() {
        Member owner = Member.createUser("owner", "encoded", "이름", null, null);
        ReflectionTestUtils.setField(owner, "id", 1L);
        loginAs(2L, "intruder");

        Store store = storeWithId(10L, owner, TODAY, TODAY, 1);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

        StoreUpdateRequest request = new StoreUpdateRequest(100L, "수정", null, null, StoreCategory.LIGHT, 1, TODAY, TODAY);

        assertThatThrownBy(() -> storeService.updateStore(10L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void updateStore_paidReservation_isRejected() {
        Member user = loginAs(1L, "user1");
        Store store = storeWithId(10L, user, TODAY, TODAY, 1);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(paidPayment(store, 3000)));

        StoreUpdateRequest request = new StoreUpdateRequest(100L, "수정", null, null, StoreCategory.LIGHT, 1, TODAY, TODAY);

        assertThatThrownBy(() -> storeService.updateStore(10L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ALREADY_PAID);
    }

    @Test
    void updateStore_excludesItselfFromCapacityCheck() {
        Member user = loginAs(1L, "user1");
        Store store = storeWithId(10L, user, TODAY, TODAY, 5); // 혼자서 수용량 5를 꽉 채운 예약
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.empty());
        when(storagePlaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(place));
        when(storeRepository.findOverlapping(any(), any(), any(), any())).thenReturn(List.of(store));

        StoreUpdateRequest request = new StoreUpdateRequest(100L, "제목만 수정", null, null, StoreCategory.LIGHT, 5, TODAY, TODAY);

        // 자기 자신을 빼지 않으면 5 + 5 > 5 로 막혀서, 제목만 바꿔도 수정이 안 된다
        assertThat(storeService.updateStore(10L, request).name()).isEqualTo("제목만 수정");
    }

    // ───────── 삭제 / 취소 ─────────

    @Test
    void deleteStore_unpaid_cascadesPaymentAndImages() {
        Member user = loginAs(1L, "user1");
        Store store = storeWithId(10L, user, TODAY, TODAY, 1);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        Payment readyPayment = new Payment(store, "order-1", 3000);
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(readyPayment));

        storeService.deleteStore(10L);

        verify(paymentRepository).delete(readyPayment);
        verify(storeImageRepository).deleteByStore(store);
        verify(storeRepository).delete(store);
    }

    @Test
    void deleteStore_paid_mustGoThroughCancel() {
        Member user = loginAs(1L, "user1");
        Store store = storeWithId(10L, user, TODAY, TODAY, 1);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(paidPayment(store, 3000)));

        assertThatThrownBy(() -> storeService.deleteStore(10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PAID_RESERVATION_REQUIRES_CANCEL);
        verify(storeRepository, never()).delete(any());
    }

    @Test
    void cancelStore_beforeStartDate_refundsInFull() {
        Member user = loginAs(1L, "user1");
        Store store = storeWithId(10L, user, TODAY.plusDays(1), TODAY.plusDays(2), 1);
        Payment payment = paidPayment(store, 6000);
        when(storeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(payment));

        StoreResponse response = storeService.cancelStore(10L);

        verify(tossPaymentsClient).cancel(eq("toss-key"), anyString(), eq(6000), anyString());
        verify(reservationNotifier).refunded(store, 6000);
        assertThat(response.status()).isEqualTo(StoreStatus.CANCELED);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(response.refundedAmount()).isEqualTo(6000);
    }

    @Test
    void cancelStore_onStartDate_refundsHalf() {
        Member user = loginAs(1L, "user1");
        Store store = storeWithId(10L, user, TODAY, TODAY.plusDays(1), 1);
        Payment payment = paidPayment(store, 6000);
        when(storeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(payment));

        StoreResponse response = storeService.cancelStore(10L);

        verify(tossPaymentsClient).cancel(eq("toss-key"), anyString(), eq(3000), anyString());
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.PARTIAL_CANCELED);
        assertThat(response.refundedAmount()).isEqualTo(3000);
    }

    @Test
    void cancelStore_afterCheckIn_isRejected() {
        Member user = loginAs(1L, "user1");
        Store store = storeWithId(10L, user, TODAY, TODAY, 1);
        store.checkIn(LocalDateTime.now());
        when(storeRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> storeService.cancelStore(10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STORE_STATUS);
        verify(tossPaymentsClient, never()).cancel(anyString(), anyString(), anyInt(), anyString());
    }
}
