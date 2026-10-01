package com.luggagestorage.member.dto;

import com.luggagestorage.member.entity.MemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 휴대폰 인증을 마친 뒤에 가입한다. verificationToken은 /api/auth/phone/verify 에서 받은 일회용 토큰.
 */
public record SignupRequest(

    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "아이디는 4~20자의 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.")
    String username,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    String password,

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 30, message = "이름은 30자 이하여야 합니다.")
    String name,

    @NotBlank(message = "휴대폰 번호는 필수입니다.")
    String phoneNumber,

    @NotBlank(message = "휴대폰 인증을 먼저 완료해주세요.")
    String verificationToken,

    /** USER(이용자) 또는 OWNER(지점 운영자). 비우면 USER */
    MemberRole role
) {
}
