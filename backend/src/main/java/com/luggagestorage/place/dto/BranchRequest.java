package com.luggagestorage.place.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 관리자가 정식 지점을 등록·수정할 때 보내는 값 (좌표는 지도 표시용, 없어도 된다) */
public record BranchRequest(

    @NotBlank(message = "지점 코드는 필수입니다.")
    @Pattern(regexp = "^[A-Z0-9_]{2,30}$", message = "지점 코드는 영문 대문자·숫자·밑줄 2~30자입니다.")
    String code,

    @NotBlank(message = "지점 이름은 필수입니다.")
    @Size(max = 100, message = "지점 이름은 100자 이하여야 합니다.")
    String name,

    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
    String address,

    @Size(max = 500, message = "소개는 500자 이하여야 합니다.")
    String description,

    @NotNull(message = "기본 수용량은 필수입니다.")
    @Positive(message = "수용량은 1 이상이어야 합니다.")
    @Max(value = 1000, message = "수용량은 1000개 이하여야 합니다.")
    Integer capacity,

    @DecimalMin(value = "33.0", message = "위도가 올바르지 않습니다.")
    @DecimalMax(value = "39.0", message = "위도가 올바르지 않습니다.")
    Double latitude,

    @DecimalMin(value = "124.0", message = "경도가 올바르지 않습니다.")
    @DecimalMax(value = "132.0", message = "경도가 올바르지 않습니다.")
    Double longitude
) {
}
