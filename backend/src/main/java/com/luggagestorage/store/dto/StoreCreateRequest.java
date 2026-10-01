package com.luggagestorage.store.dto;

import com.luggagestorage.store.entity.StoreCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

/** 시작 날짜가 오늘 이후인지는 서비스에서 한국 시간 기준으로 확인한다 (서버가 UTC여도 같게 동작) */
public record StoreCreateRequest(

    @NotNull(message = "보관소를 선택해주세요.")
    Long placeId,

    @NotBlank(message = "제목은 필수입니다.")
    String name,

    String description,

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
