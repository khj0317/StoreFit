package com.luggagestorage.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalTime;
import java.util.List;

public record StoreCreateRequest(

    @NotBlank(message = "보관소 이름은 필수입니다.")
    String name,

    String description,

    @NotBlank(message = "주소는 필수입니다.")
    String address,

    Double latitude,

    Double longitude,

    @NotNull(message = "시간당 가격은 필수입니다.")
    @Positive(message = "시간당 가격은 0보다 커야 합니다.")
    Integer pricePerHour,

    @NotNull(message = "수용 가능 개수는 필수입니다.")
    @Positive(message = "수용 가능 개수는 0보다 커야 합니다.")
    Integer capacity,

    LocalTime openTime,

    LocalTime closeTime,

    List<String> imageUrls
) {
}
