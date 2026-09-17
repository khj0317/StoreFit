package com.luggagestorage.reservation.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record ReservationCreateRequest(

    @NotNull(message = "보관소 ID는 필수입니다.")
    Long storeId,

    @NotNull(message = "짐 개수는 필수입니다.")
    @Positive(message = "짐 개수는 0보다 커야 합니다.")
    Integer luggageCount,

    @NotNull(message = "시작 시간은 필수입니다.")
    @Future(message = "시작 시간은 현재 시간 이후여야 합니다.")
    LocalDateTime startTime,

    @NotNull(message = "종료 시간은 필수입니다.")
    LocalDateTime endTime
) {
}
