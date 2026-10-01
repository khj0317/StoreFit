package com.luggagestorage.store.dto;

/**
 * 취소하면 돌려받을 금액 미리보기.
 * @param refundRate 0~100 (%)
 */
public record RefundPreviewResponse(Integer paidAmount, Integer refundAmount, Integer refundRate, String policy) {
}
