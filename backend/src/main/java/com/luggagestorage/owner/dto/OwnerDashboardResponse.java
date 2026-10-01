package com.luggagestorage.owner.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * @param arrivals 오늘 짐을 맡기러 올 (결제했고 아직 체크인 안 한) 예약
 * @param departures 오늘까지 찾아가야 하는 보관 중 예약 (기간이 지난 것 포함)
 * @param storedLuggageCount 지금 보관 중인 짐 개수
 * @param monthRevenue 이번 달 결제 금액에서 환불 금액을 뺀 매출
 */
public record OwnerDashboardResponse(
    LocalDate today,
    List<OwnerReservationResponse> arrivals,
    List<OwnerReservationResponse> departures,
    int storedLuggageCount,
    long monthRevenue,
    List<PlaceOccupancy> places
) {

    public record PlaceOccupancy(Long id, String name, String address, Integer capacity, Integer occupiedToday) {
    }
}
