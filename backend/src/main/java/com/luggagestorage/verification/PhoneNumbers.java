package com.luggagestorage.verification;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;

import java.util.regex.Pattern;

/** 휴대폰 번호는 숫자만 남겨서(01012345678) 저장·비교한다 */
public final class PhoneNumbers {

    private static final Pattern MOBILE = Pattern.compile("^01[016789]\\d{7,8}$");

    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (!MOBILE.matcher(digits).matches()) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
        }
        return digits;
    }

    /** 010-1234-5678 형태로 보여준다 */
    public static String format(String digits) {
        if (digits == null) {
            return null;
        }
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        return digits;
    }
}
