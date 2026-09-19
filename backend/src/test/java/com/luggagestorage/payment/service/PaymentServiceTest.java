package com.luggagestorage.payment.service;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.client.TossConfirmResult;
import com.luggagestorage.payment.client.TossPaymentsClient;
import com.luggagestorage.payment.dto.PaymentConfirmRequest;
import com.luggagestorage.payment.dto.PaymentReadyResponse;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.entity.PaymentStatus;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TossPaymentsClient tossPaymentsClient;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, storeRepository, memberRepository, tossPaymentsClient);
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

    private Store storeWithId(Long id, Member owner, int totalPrice) {
        Store store = new Store(owner, "짐", null, "주소", StoreCategory.LIGHT, 1, LocalDate.now(), LocalDate.now(), totalPrice);
        ReflectionTestUtils.setField(store, "id", id);
        return store;
    }

    @Test
    void ready_notOwner_throwsAccessDenied() {
        Member owner = memberWithId(1L, "owner");
        Member intruder = memberWithId(2L, "intruder");
        loginAs("intruder");
        when(memberRepository.findByUsername("intruder")).thenReturn(Optional.of(intruder));

        Store store = storeWithId(10L, owner, 3000);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> paymentService.ready(10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void ready_alreadyPaid_throws() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, 3000);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

        Payment donePayment = new Payment(store, "order-1", 3000);
        donePayment.approve("key", "카드", java.time.LocalDateTime.now());
        when(paymentRepository.findByStore(store)).thenReturn(Optional.of(donePayment));

        assertThatThrownBy(() -> paymentService.ready(10L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ALREADY_PAID);
    }

    @Test
    void ready_noExistingPayment_createsNewReadyPayment() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, 3000);
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(paymentRepository.findByStore(store)).thenReturn(Optional.empty());
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentReadyResponse response = paymentService.ready(10L);

        assertThat(response.amount()).isEqualTo(3000);
        assertThat(response.orderName()).isEqualTo("짐");
    }

    @Test
    void confirm_amountMismatch_throws() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, 3000);
        Payment payment = new Payment(store, "order-1", 3000);
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        PaymentConfirmRequest request = new PaymentConfirmRequest("key", "order-1", 9999);

        assertThatThrownBy(() -> paymentService.confirm(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_PAYMENT_AMOUNT);
    }

    @Test
    void confirm_notOwner_throwsAccessDenied() {
        Member owner = memberWithId(1L, "owner");
        Member intruder = memberWithId(2L, "intruder");
        loginAs("intruder");
        when(memberRepository.findByUsername("intruder")).thenReturn(Optional.of(intruder));

        Store store = storeWithId(10L, owner, 3000);
        Payment payment = new Payment(store, "order-1", 3000);
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        PaymentConfirmRequest request = new PaymentConfirmRequest("key", "order-1", 3000);

        assertThatThrownBy(() -> paymentService.confirm(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    void confirm_success_approvesPaymentViaToss() {
        Member owner = memberWithId(1L, "owner");
        loginAs("owner");
        when(memberRepository.findByUsername("owner")).thenReturn(Optional.of(owner));

        Store store = storeWithId(10L, owner, 3000);
        Payment payment = new Payment(store, "order-1", 3000);
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        TossConfirmResult tossResult = new TossConfirmResult(
            "toss-key", "order-1", 3000, "카드", "2026-09-18T00:00:00+09:00", "DONE"
        );
        when(tossPaymentsClient.confirm(eq("toss-key"), eq("order-1"), eq(3000))).thenReturn(tossResult);

        PaymentConfirmRequest request = new PaymentConfirmRequest("toss-key", "order-1", 3000);

        var response = paymentService.confirm(request);

        assertThat(response.status()).isEqualTo(PaymentStatus.DONE);
        assertThat(response.method()).isEqualTo("카드");
    }
}
