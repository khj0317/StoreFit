package com.luggagestorage.member.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePhoneRequest(

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    String phoneNumber,

    @NotBlank(message = "휴대폰 인증을 먼저 완료해주세요.")
    String verificationToken
) {
}
