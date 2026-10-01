package com.luggagestorage.verification.dto;

/**
 * @param devCode 문자가 실제로 가지 않는 개발 환경에서만 인증번호를 담아 준다 (배포에서는 항상 null)
 */
public record SendCodeResponse(long expiresInSeconds, String devCode) {
}
