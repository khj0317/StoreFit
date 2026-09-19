package com.luggagestorage.store.service;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.store.dto.StoreCreateRequest;
import com.luggagestorage.store.dto.StoreResponse;
import com.luggagestorage.store.dto.StoreUpdateRequest;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;
    @Mock
    private StoreImageRepository storeImageRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private MemberRepository memberRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(storeRepository, storeImageRepository, paymentRepository, memberRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null)
        );
    }

    private Member memberWithId(Long id, String username) {
        Member member = Member.createUser(username, "encoded", "이름", null, null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Store storeWithId(Long id, Member owner, StoreCategory category, LocalDate start, LocalDate end, int totalPrice) {
        Store store = new Store(owner, "짐", null, "주소", category, 2, start, end, totalPrice);
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

    @Test
    void createStore_calculatesTotalPriceFromCategoryLuggageCountAndDays() {
        Member member = memberWithId(1L, "user1");
        loginAs("user1");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));
        when(paymentRepository.findByStore(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(2); // 3-day span inclusive
        StoreCreateRequest request = new StoreCreateRequest(
            "짐", null, "주소", List.of(), StoreCategory.LIGHT, 2, start, end
        );

        StoreResponse response = storeService.createStore(request);

        // LIGHT dailyRate 3000 * 2 luggage * 3 days = 18000
        assertThat(response.totalPrice()).isEqualTo(18_000);
    }

    @Test
    void createStore_endBeforeStart_throws() {
        Member member = memberWithId(1L, "user1");
        loginAs("user1");
        when(memberRepository.findByUsername("user1")).thenReturn(Optional.of(member));

        LocalDate start = LocalDate.now().plusDays(3);
        LocalDate end = LocalDate.now();
        StoreCreateRequest request = new StoreCreateRequest(
            "짐", null, "주소", List.of(), StoreCategory.LIGHT, 1, start, end
        );

        assertThatThrownBy(() -> storeService.createStore(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STORE_TIME);
    }

    @Test
    void updateStore_notOwner_throwsAccessDenied() {
        Member owner = memberWithId(1L, "owner");
        Member intruder = memberWithId(2L, "intruder");
        loginAs("intruder");
        when(memberRepository.findByUsername("intruder")).thenReturn(Optional.of(intruder));

        Store store = storeWithId(10L, owner, StoreCategory.LIGHT, LocalDate.now(), LocalDate.now(), 3000);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

        StoreUpdateRequest request = new StoreUpdateRequest(
            "수정", null, "주소", null, StoreCategory.LIGHT, 1, LocalDate.now(), LocalDate.now()
        );

        assertThatThrownBy(() -> storeService.updateStore(10L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void updateStore_notPending_throwsInvalidStatus() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, StoreCategory.LIGHT, LocalDate.now(), LocalDate.now(), 3000);
        store.pickUp(); // now PICKED_UP, not PENDING
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

        StoreUpdateRequest request = new StoreUpdateRequest(
            "수정", null, "주소", null, StoreCategory.LIGHT, 1, LocalDate.now(), LocalDate.now()
        );

        assertThatThrownBy(() -> storeService.updateStore(10L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_STORE_STATUS);
    }

    @Test
    void pickUpStore_withoutPayment_throwsPaymentRequired() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, StoreCategory.LIGHT, LocalDate.now(), LocalDate.now(), 3000);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeService.pickUpStore(10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.PAYMENT_REQUIRED);
    }

    @Test
    void pickUpStore_paid_transitionsToPickedUp() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, StoreCategory.LIGHT, LocalDate.now(), LocalDate.now(), 3000);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

        Payment donePayment = new Payment(store, "order-1", 3000);
        donePayment.approve("key", "카드", java.time.LocalDateTime.now());
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(donePayment));
        when(storeImageRepository.findByStoreOrderBySortOrderAsc(store)).thenReturn(List.of());

        StoreResponse response = storeService.pickUpStore(10L);

        assertThat(response.status()).isEqualTo(com.luggagestorage.store.entity.StoreStatus.PICKED_UP);
    }

    @Test
    void deleteStore_cascadesPaymentAndImages() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, StoreCategory.LIGHT, LocalDate.now(), LocalDate.now(), 3000);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        Payment payment = new Payment(store, "order-1", 3000);
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(payment));

        storeService.deleteStore(10L);

        org.mockito.Mockito.verify(paymentRepository).delete(payment);
        org.mockito.Mockito.verify(storeImageRepository).deleteByStore(store);
        org.mockito.Mockito.verify(storeRepository).delete(store);
    }
}
