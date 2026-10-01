package com.luggagestorage.verification.dto;

import com.luggagestorage.verification.VerificationPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PhoneCodeRequests {

    private PhoneCodeRequests() {
    }

    public record Send(
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        String phoneNumber,

        @NotNull(message = "인증 목적은 필수입니다.")
        VerificationPurpose purpose
    ) {
    }

    public record Verify(
        @NotBlank(message = "휴대폰 번호는 필수입니다.")
        String phoneNumber,

        @NotNull(message = "인증 목적은 필수입니다.")
        VerificationPurpose purpose,

        @NotBlank(message = "인증번호를 입력해주세요.")
        String code
    ) {
    }

    public record Verified(String verificationToken) {
    }
}
