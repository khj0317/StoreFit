package com.luggagestorage.payment.entity;

public enum PaymentStatus {
    READY,
    DONE,
    FAILED,
    /** 전액 환불 */
    CANCELED,
    /** 일부만 환불 (취소 수수료를 뗀 경우) */
    PARTIAL_CANCELED
}
