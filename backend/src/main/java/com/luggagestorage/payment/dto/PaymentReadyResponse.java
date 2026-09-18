package com.luggagestorage.payment.dto;

public record PaymentReadyResponse(
    String orderId,
    Integer amount,
    String orderName
) {
}
