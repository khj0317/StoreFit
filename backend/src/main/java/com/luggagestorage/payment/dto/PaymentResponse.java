package com.luggagestorage.payment.dto;

import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
    Long id,
    Long storeId,
    String storeName,
    String orderId,
    Integer amount,
    PaymentStatus status,
    String method,
    LocalDateTime approvedAt,
    LocalDateTime createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getStore().getId(),
            payment.getStore().getName(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getMethod(),
            payment.getApprovedAt(),
            payment.getCreatedAt()
        );
    }
}
