package com.luggagestorage.payment.service;

import java.time.LocalDate;

/**
 * 취소 환불 규칙.
 * - 보관 시작 전날까지 취소: 전액 환불
 * - 보관 시작일 당일(또는 그 이후, 체크인 전) 취소: 50% 환불
 * 운영자가 그날 자리를 비워둔 비용을 반영한 규칙이다.
 */
public final class RefundPolicy {

    public static final String DESCRIPTION = "보관 시작 전날까지 취소하면 전액, 시작일 당일부터는 50%를 환불해 드려요.";

    private RefundPolicy() {
    }

    public static int refundRate(LocalDate today, LocalDate startDate) {
        return today.isBefore(startDate) ? 100 : 50;
    }

    public static int refundAmount(int paidAmount, LocalDate today, LocalDate startDate) {
        return paidAmount * refundRate(today, startDate) / 100;
    }
}
