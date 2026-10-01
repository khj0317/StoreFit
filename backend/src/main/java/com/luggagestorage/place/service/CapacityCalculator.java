package com.luggagestorage.place.service;

import com.luggagestorage.store.entity.Store;

import java.time.LocalDate;
import java.util.Collection;

/**
 * 보관소 자리 계산. 예약은 날짜 범위라서, 기간 안의 하루하루마다 맡겨진 짐 개수를 더해
 * 가장 붐비는 날의 개수(peak)를 구한다. 새 예약은 peak + 짐 개수가 capacity 이하일 때만 받는다.
 */
public final class CapacityCalculator {

    private CapacityCalculator() {
    }

    /**
     * @param stores 이미 걸러낸, 기간이 겹치는 진행 중 예약들
     * @return [start, end] 안에서 가장 많이 맡겨진 날의 짐 개수
     */
    public static int peakOccupancy(Collection<Store> stores, LocalDate start, LocalDate end) {
        int peak = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            int occupied = 0;
            for (Store store : stores) {
                if (!day.isBefore(store.getStartDate()) && !day.isAfter(store.getEndDate())) {
                    occupied += store.getLuggageCount();
                }
            }
            peak = Math.max(peak, occupied);
        }
        return peak;
    }
}
