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
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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

    @Transactional
    public PaymentReadyResponse ready(Long storeId) {
        Member member = getCurrentMember();
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
        if (!store.isOwnedBy(member.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Optional<Payment> existing = paymentRepository.findByStore(store);
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.DONE) {
            throw new BusinessException(ErrorCode.ALREADY_PAID);
        }

        Payment payment = existing
            .filter(p -> p.getStatus() == PaymentStatus.READY)
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

        TossConfirmResult result = tossPaymentsClient.confirm(request.paymentKey(), request.orderId(), request.amount());
        payment.approve(result.paymentKey(), result.method(), OffsetDateTime.parse(result.approvedAt()).toLocalDateTime());

        return PaymentResponse.from(payment);
    }

    private String generateOrderId(Store store) {
        return "store-" + store.getId() + "-" + UUID.randomUUID();
    }

    private Member getCurrentMember() {
        String email = SecurityUtil.getCurrentMemberEmail();
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
