package com.luggagestorage.store.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record StoreUpdateRequest(

    @NotBlank(message = "제목은 필수입니다.")
    String name,

    String description,

    @NotBlank(message = "주소는 필수입니다.")
    String address,

    List<String> imageUrls
) {
}
