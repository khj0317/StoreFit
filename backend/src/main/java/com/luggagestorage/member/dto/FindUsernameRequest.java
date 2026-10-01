package com.luggagestorage.member.dto;

import jakarta.validation.constraints.NotBlank;

/** 가입한 휴대폰 번호로 인증하면 아이디를 알려준다 */
public record FindUsernameRequest(

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    String phoneNumber,

    @NotBlank(message = "휴대폰 인증을 먼저 완료해주세요.")
    String verificationToken
) {
}
