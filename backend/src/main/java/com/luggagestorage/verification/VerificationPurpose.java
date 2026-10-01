package com.luggagestorage.verification;

/** 인증번호를 어디에 쓰는지. 가입용 인증으로 비밀번호를 바꾸는 식의 재사용을 막는다 */
public enum VerificationPurpose {
    SIGNUP,
    FIND_USERNAME,
    RESET_PASSWORD,
    CHANGE_PHONE
}
