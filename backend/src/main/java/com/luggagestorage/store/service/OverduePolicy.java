package com.luggagestorage.store.service;

import com.luggagestorage.store.entity.Store;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 찾는 날이 지나도 짐을 찾아가지 않으면 지난 날짜만큼 하루 요금(짐 종류 요금 × 개수)을 연체료로 받는다.
 * 연체료는 체크아웃할 때 지점에서 현장 결제한다.
 */
public final class OverduePolicy {

    private OverduePolicy() {
    }

    public static long overdueDays(Store store, LocalDate today) {
        return Math.max(0, ChronoUnit.DAYS.between(store.getEndDate(), today));
    }

    public static int dailyFee(Store store) {
        return store.getCategory().getDailyRate() * store.getLuggageCount();
    }

    public static long overdueFee(Store store, LocalDate today) {
        return overdueDays(store, today) * dailyFee(store);
    }
}
