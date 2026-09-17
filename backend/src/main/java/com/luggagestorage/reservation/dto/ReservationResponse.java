package com.luggagestorage.reservation.dto;

import com.luggagestorage.reservation.entity.Reservation;
import com.luggagestorage.reservation.entity.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationResponse(
    Long id,
    Long storeId,
    String storeName,
    Long memberId,
    String memberName,
    Integer luggageCount,
    LocalDateTime startTime,
    LocalDateTime endTime,
    ReservationStatus status,
    LocalDateTime createdAt
) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getStore().getId(),
            reservation.getStore().getName(),
            reservation.getMember().getId(),
            reservation.getMember().getName(),
            reservation.getLuggageCount(),
            reservation.getStartTime(),
            reservation.getEndTime(),
            reservation.getStatus(),
            reservation.getCreatedAt()
        );
    }
}
