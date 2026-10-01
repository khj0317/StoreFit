package com.luggagestorage.place.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 운영 신청: 운영할 수용량·소개와 관리자에게 남길 말 */
public record BranchApplyRequest(

    @NotNull(message = "수용량은 필수입니다.")
    @Positive(message = "수용량은 1 이상이어야 합니다.")
    @Max(value = 1000, message = "수용량은 1000개 이하여야 합니다.")
    Integer capacity,

    @Size(max = 500, message = "소개는 500자 이하여야 합니다.")
    String description,

    @Size(max = 500, message = "남기는 말은 500자 이하여야 합니다.")
    String message
) {
}
