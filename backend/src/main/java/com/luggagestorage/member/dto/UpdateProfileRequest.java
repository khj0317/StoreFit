package com.luggagestorage.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 휴대폰 번호는 인증이 필요해서 따로 바꾼다 (ChangePhoneRequest) */
public record UpdateProfileRequest(

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
    String name
) {
}
