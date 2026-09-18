package com.luggagestorage.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public record StoreUpdateRequest(

    @NotBlank(message = "제목은 필수입니다.")
    String name,

    String description,

    @NotBlank(message = "주소는 필수입니다.")
    String address,

    List<String> imageUrls,

    @NotNull(message = "짐 개수는 필수입니다.")
    @Positive(message = "짐 개수는 0보다 커야 합니다.")
    Integer luggageCount,

    @NotNull(message = "시작 시간은 필수입니다.")
    LocalDateTime startTime,

    @NotNull(message = "종료 시간은 필수입니다.")
    LocalDateTime endTime
) {
}
