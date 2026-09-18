package com.luggagestorage.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(

    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "아이디는 4~20자의 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.")
    String username,

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    String password,

    @NotBlank(message = "이름은 필수입니다.")
    String name,

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    String email,

    String phoneNumber
) {
}
