package com.luggagestorage.payment.dto;

import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
    Long id,
    Long storeId,
    String orderId,
    Integer amount,
    PaymentStatus status,
    String method,
    LocalDateTime approvedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getStore().getId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getMethod(),
            payment.getApprovedAt()
        );
    }
}
