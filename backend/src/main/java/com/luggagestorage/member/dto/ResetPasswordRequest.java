package com.luggagestorage.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 아이디와, 그 계정에 등록된 휴대폰 번호 인증으로 비밀번호를 바꾼다 */
public record ResetPasswordRequest(

    @NotBlank(message = "아이디는 필수입니다.")
    String username,

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    String phoneNumber,

    @NotBlank(message = "휴대폰 인증을 먼저 완료해주세요.")
    String verificationToken,

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    String newPassword
) {
}
