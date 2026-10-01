package com.luggagestorage.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "휴대폰 번호 형식이 올바르지 않습니다."),
    PHONE_NOT_REGISTERED(HttpStatus.NOT_FOUND, "이 번호로 가입한 계정이 없습니다."),
    VERIFICATION_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분 뒤에 다시 받을 수 있어요."),
    VERIFICATION_TOO_MANY(HttpStatus.TOO_MANY_REQUESTS, "인증번호 요청이 너무 많아요. 1시간 뒤에 다시 시도해주세요."),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료됐거나 입력 횟수를 넘었어요. 인증번호를 다시 받아주세요."),
    VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "휴대폰 인증이 만료됐어요. 다시 인증해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해주세요."),
    ACTIVE_RESERVATION_EXISTS(HttpStatus.CONFLICT, "결제했거나 맡겨둔 짐이 있어 탈퇴할 수 없습니다. 보관을 마친 뒤 탈퇴해주세요."),
    BRANCH_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 운영 신청입니다."),
    BRANCH_APPLICATION_DUPLICATE(HttpStatus.CONFLICT, "이미 심사 중인 신청이 있습니다."),
    BRANCH_APPLICATION_DECIDED(HttpStatus.CONFLICT, "이미 처리된 신청입니다."),
    DUPLICATE_BRANCH_CODE(HttpStatus.CONFLICT, "이미 사용 중인 지점 코드입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    IDENTITY_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "입력하신 정보와 일치하는 계정을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "선택할 수 없는 회원 유형입니다."),
    OWNER_HAS_PLACES(HttpStatus.CONFLICT, "운영 중인 보관소가 있어 탈퇴할 수 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 짐 보관입니다."),
    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 보관소입니다."),
    BRANCH_ALREADY_TAKEN(HttpStatus.CONFLICT, "이미 다른 운영자가 맡은 지점입니다."),
    PLACE_FULL(HttpStatus.CONFLICT, "선택한 기간에 보관소 자리가 부족합니다. 다른 날짜나 보관소를 선택해주세요."),
    PLACE_CAPACITY_TOO_SMALL(HttpStatus.CONFLICT, "이미 받은 예약보다 수용량을 작게 줄일 수 없습니다."),
    STORAGE_PERIOD_TOO_LONG(HttpStatus.BAD_REQUEST, "보관 기간은 최대 60일까지 예약할 수 있습니다."),
    CHECK_IN_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "체크인 코드를 찾을 수 없습니다."),
    CHECK_IN_DATE_INVALID(HttpStatus.BAD_REQUEST, "보관 시작일 당일에만 체크인할 수 있습니다."),
    PAID_RESERVATION_REQUIRES_CANCEL(HttpStatus.CONFLICT, "결제한 예약은 취소(환불) 요청으로 처리해주세요."),
    REFUND_FAILED(HttpStatus.BAD_GATEWAY, "환불 처리에 실패했습니다. 잠시 후 다시 시도해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_STORE_TIME(HttpStatus.BAD_REQUEST, "시작/종료 날짜가 올바르지 않습니다."),
    INVALID_STORE_STATUS(HttpStatus.BAD_REQUEST, "처리할 수 없는 상태입니다."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "이미지 파일(JPG, PNG, WEBP, GIF, HEIC)만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "사진은 한 장에 10MB, 한 번에 30MB까지 올릴 수 있습니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "같은 정보로 이미 처리된 요청이 있어요. 잠시 후 다시 확인해주세요."),
    TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "비밀번호를 여러 번 틀려 10분 동안 로그인할 수 없어요. 비밀번호 찾기를 이용해주세요."),
    TOO_MANY_FILES(HttpStatus.BAD_REQUEST, "사진은 한 번에 10장까지 올릴 수 있습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    PAYMENT_DEADLINE_PASSED(HttpStatus.CONFLICT, "결제 시간(예약 후 30분)이 지나 예약이 취소됐어요. 다시 예약해주세요."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 결제입니다."),
    PAYMENT_REQUIRED(HttpStatus.PAYMENT_REQUIRED, "결제가 완료된 예약만 체크인할 수 있습니다."),
    ALREADY_PAID(HttpStatus.CONFLICT, "이미 결제가 완료되었습니다."),
    INVALID_PAYMENT_AMOUNT(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "결제 승인에 실패했습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다.");

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
