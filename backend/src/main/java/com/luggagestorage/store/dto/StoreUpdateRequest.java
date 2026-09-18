package com.luggagestorage.store.dto;

import com.luggagestorage.store.entity.StoreCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record StoreUpdateRequest(

    @NotBlank(message = "제목은 필수입니다.")
    String name,

    String description,

    @NotBlank(message = "주소는 필수입니다.")
    String address,

    List<String> imageUrls,

    @NotNull(message = "짐 종류는 필수입니다.")
    StoreCategory category,

    @NotNull(message = "짐 개수는 필수입니다.")
    @Positive(message = "짐 개수는 0보다 커야 합니다.")
    Integer luggageCount,

    @NotNull(message = "시작 날짜는 필수입니다.")
    LocalDate startDate,

    @NotNull(message = "종료 날짜는 필수입니다.")
    LocalDate endDate
) {
}
