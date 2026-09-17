package com.luggagestorage.common.exception;

public record ErrorResponse(int status, String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, "VALIDATION_ERROR", message);
    }
}
