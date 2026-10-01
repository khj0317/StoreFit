package com.luggagestorage.place.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 운영자가 지점을 맡거나 운영 정보를 바꿀 때 보내는 값.
 * 이름·주소는 스토어핏 본사가 등록한 정보라 여기서 받지 않는다.
 * @param description 비우면 본사가 등록한 소개를 그대로 쓴다
 */
public record PlaceRequest(

    @NotNull(message = "수용량은 필수입니다.")
    @Positive(message = "수용량은 1 이상이어야 합니다.")
    @Max(value = 1000, message = "수용량은 1000개 이하여야 합니다.")
    Integer capacity,

    @Size(max = 500, message = "소개는 500자 이하여야 합니다.")
    String description
) {
}
