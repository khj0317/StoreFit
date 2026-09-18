package com.luggagestorage.payment.client;

public record TossConfirmResult(
    String paymentKey,
    String orderId,
    Integer totalAmount,
    String method,
    String approvedAt,
    String status
) {
}
