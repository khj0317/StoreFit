package com.luggagestorage.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 짐 보관입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_STORE_TIME(HttpStatus.BAD_REQUEST, "시작/종료 시간이 올바르지 않습니다."),
    INVALID_STORE_STATUS(HttpStatus.BAD_REQUEST, "처리할 수 없는 상태입니다."),
    STORE_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 짐 보관에만 리뷰를 작성할 수 있습니다."),
    DUPLICATE_REVIEW(HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
