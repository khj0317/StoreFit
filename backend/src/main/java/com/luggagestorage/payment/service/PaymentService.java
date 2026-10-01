package com.luggagestorage.payment.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.client.TossConfirmResult;
import com.luggagestorage.payment.client.TossPaymentsClient;
import com.luggagestorage.payment.dto.PaymentConfirmRequest;
import com.luggagestorage.payment.dto.PaymentReadyResponse;
import com.luggagestorage.payment.dto.PaymentResponse;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.entity.PaymentStatus;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.service.CheckInCodeGenerator;
import com.luggagestorage.store.service.ReservationNotifier;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final CheckInCodeGenerator checkInCodeGenerator;
    private final ReservationNotifier reservationNotifier;
    private final Clock clock;

    /** 결제창을 연 사람에게는 최소 이만큼의 시간을 보장한다 (결제 중에 자동 취소되지 않도록) */
    static final Duration MIN_TIME_TO_PAY = Duration.ofMinutes(10);

    @Transactional
    public PaymentReadyResponse ready(Long storeId) {
        Member member = getCurrentMember();
        Store store = storeRepository.findByIdForUpdate(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
        if (!store.isOwnedBy(member.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (store.getStatus() == StoreStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.PAYMENT_DEADLINE_PASSED);
        }
        if (store.getStatus() != StoreStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STORE_STATUS);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (store.isPaymentDeadlinePassed(now)) {
            throw new BusinessException(ErrorCode.PAYMENT_DEADLINE_PASSED);
        }
        if (store.getPaymentDeadline() != null && store.getPaymentDeadline().isBefore(now.plus(MIN_TIME_TO_PAY))) {
            store.setPaymentDeadline(now.plus(MIN_TIME_TO_PAY));
        }

        Optional<Payment> existing = paymentRepository.findByStore(store);
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.DONE) {
            throw new BusinessException(ErrorCode.ALREADY_PAID);
        }

        // 결제 전에 예약을 수정해 금액이 바뀌었으면 예전 주문은 버리고 새 주문번호로 받는다
        existing.filter(p -> !p.getAmount().equals(store.getTotalPrice())).ifPresent(stale -> {
            paymentRepository.delete(stale);
            paymentRepository.flush();
        });
        Payment payment = existing
            .filter(p -> p.getStatus() == PaymentStatus.READY && p.getAmount().equals(store.getTotalPrice()))
            .orElseGet(() -> paymentRepository.save(new Payment(store, generateOrderId(store), store.getTotalPrice())));

        return new PaymentReadyResponse(payment.getOrderId(), payment.getAmount(), store.getName());
    }

    @Transactional
    public PaymentResponse confirm(PaymentConfirmRequest request) {
        Member member = getCurrentMember();
        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (!payment.isOwnedBy(member.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (!payment.getAmount().equals(request.amount())) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_AMOUNT);
        }
        if (payment.getStatus() == PaymentStatus.DONE) {
            throw new BusinessException(ErrorCode.ALREADY_PAID);
        }
        // 자동 취소와 동시에 일어나지 않도록 예약을 잠그고 아직 결제할 수 있는 상태인지 다시 본다.
        // 여기서 막히면 결제 승인 API를 부르지 않으므로 돈이 빠져나가지 않는다
        Store store = storeRepository.findByIdForUpdate(payment.getStore().getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
        if (store.getStatus() != StoreStatus.PENDING) {
            throw new BusinessException(store.getStatus() == StoreStatus.EXPIRED
                ? ErrorCode.PAYMENT_DEADLINE_PASSED : ErrorCode.INVALID_STORE_STATUS);
        }

        TossConfirmResult result = tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), request.amount());
        payment.approve(result.paymentKey(), result.method(), OffsetDateTime.parse(result.approvedAt()).toLocalDateTime());
        // 결제가 끝난 예약에만 체크인 QR을 준다. 운영자는 이 코드로 결제 여부까지 함께 확인한다
        store.issueCheckInCode(checkInCodeGenerator.generate());
        reservationNotifier.paymentDone(store);

        return PaymentResponse.from(payment);
    }

    public List<PaymentResponse> getMyPayments() {
        Member member = getCurrentMember();
        return paymentRepository.findByStore_MemberOrderByCreatedAtDesc(member).stream()
            .map(PaymentResponse::from)
            .toList();
    }

    private String generateOrderId(Store store) {
        return "store-" + store.getId() + "-" + UUID.randomUUID();
    }

    private Member getCurrentMember() {
        String username = SecurityUtil.getCurrentUsername();
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
