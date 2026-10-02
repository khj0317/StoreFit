package com.luggagestorage.demo;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.payment.entity.Payment;

import java.util.Set;

/**
 * 로그인 화면의 "둘러보기"로 들어가는 체험 계정. 비밀번호는 서버가 켜질 때마다 아무도 모르는 값으로 정해서
 * 아이디·비밀번호로는 로그인할 수 없고, 체험하기 API로만 들어갈 수 있다.
 * 번호는 실제로 쓰이지 않는 번호라 체험 계정에는 문자를 보내지 않는다.
 */
public final class DemoAccounts {

    public static final String USER_USERNAME = "demo_user";
    public static final String OWNER_USERNAME = "demo_owner";
    static final String USER_PHONE = "01000000001";
    static final String OWNER_PHONE = "01000000000";
    /** 체험 데이터로 만든 결제. 실제 결제사에 없는 결제라 환불할 때 결제사를 부르지 않는다 */
    static final String PAYMENT_KEY_PREFIX = "demo_";

    private static final Set<String> USERNAMES = Set.of(USER_USERNAME, OWNER_USERNAME);

    private DemoAccounts() {
    }

    public static boolean isDemo(Member member) {
        return member != null && USERNAMES.contains(member.getUsername());
    }

    public static boolean isDemoPayment(Payment payment) {
        return payment.getPaymentKey() != null && payment.getPaymentKey().startsWith(PAYMENT_KEY_PREFIX);
    }
}
