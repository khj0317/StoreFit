package com.luggagestorage.auth;

import com.jayway.jsonpath.JsonPath;
import com.luggagestorage.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 휴대폰 인증으로 가입·아이디 찾기·비밀번호 재설정, 그리고 리프레시 토큰 */
class PhoneAuthTest extends IntegrationTest {

    private String send(String phone, String purpose) throws Exception {
        String body = call(HttpMethod.POST, "/api/auth/phone/send", """
            {"phoneNumber": "%s", "purpose": "%s"}
            """.formatted(phone, purpose), null)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.devCode");
    }

    private String signupJson(String username, String phone, String token) {
        return """
            {"username": "%s", "password": "%s", "name": "테스트", "phoneNumber": "%s", "verificationToken": "%s"}
            """.formatted(username, PASSWORD, phone, token);
    }

    private static String newUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test
    void signupWithoutPhoneVerification_isRejected() throws Exception {
        call(HttpMethod.POST, "/api/auth/signup", signupJson(newUsername(), uniquePhone(), "made-up-token"), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PHONE_NOT_VERIFIED"));
    }

    @Test
    void verificationToken_isSingleUse_andBoundToThatPhone() throws Exception {
        String phone = uniquePhone();
        String token = verifiedToken(phone, "SIGNUP");

        // 다른 번호로는 쓸 수 없다
        call(HttpMethod.POST, "/api/auth/signup", signupJson(newUsername(), uniquePhone(), token), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PHONE_NOT_VERIFIED"));

        call(HttpMethod.POST, "/api/auth/signup", signupJson(newUsername(), phone, token), null)
            .andExpect(status().isCreated());
        // 같은 토큰으로 두 번째 계정을 만들 수 없다
        call(HttpMethod.POST, "/api/auth/signup", signupJson(newUsername(), phone, token), null)
            .andExpect(status().isBadRequest());
    }

    @Test
    void alreadyRegisteredPhone_cannotRequestSignupCode() throws Exception {
        Login user = signup("USER");
        call(HttpMethod.POST, "/api/auth/phone/send", """
            {"phoneNumber": "%s", "purpose": "SIGNUP"}
            """.formatted(user.phone()), null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_PHONE"));
    }

    @Test
    void wrongCodeFiveTimes_locksTheCode() throws Exception {
        String phone = uniquePhone();
        String code = send(phone, "SIGNUP");
        String wrong = code.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 5; i++) {
            call(HttpMethod.POST, "/api/auth/phone/verify", """
                {"phoneNumber": "%s", "purpose": "SIGNUP", "code": "%s"}
                """.formatted(phone, wrong), null)
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_MISMATCH"));
        }
        // 다섯 번 틀리면 맞는 번호를 넣어도 통과하지 못한다 (무작위 대입 방지)
        call(HttpMethod.POST, "/api/auth/phone/verify", """
            {"phoneNumber": "%s", "purpose": "SIGNUP", "code": "%s"}
            """.formatted(phone, code), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VERIFICATION_EXPIRED"));
    }

    @Test
    void verificationCode_isNeverStoredInPlainText() throws Exception {
        String phone = uniquePhone();
        String code = send(phone, "SIGNUP");

        // 발송 기록에도, 인증 테이블에도 인증번호 원문이 남지 않는다
        List<String> logged = jdbcTemplate.queryForList(
            "select content from notifications where phone_number = ?", String.class, phone);
        assertThat(logged).isNotEmpty().allSatisfy(content -> assertThat(content).doesNotContain(code).contains("******"));
        List<String> hashes = jdbcTemplate.queryForList(
            "select code_hash from phone_verifications where phone_number = ?", String.class, phone);
        assertThat(hashes).isNotEmpty().allSatisfy(hash -> assertThat(hash).isNotEqualTo(code));
    }

    @Test
    void fiveWrongPasswords_lockLoginForAWhile() throws Exception {
        Login user = signup("USER");
        for (int i = 0; i < 5; i++) {
            call(HttpMethod.POST, "/api/auth/login", """
                {"username": "%s", "password": "wrong-password"}
                """.formatted(user.username()), null)
                .andExpect(status().isUnauthorized());
        }
        // 맞는 비밀번호여도 잠긴 동안은 막힌다
        call(HttpMethod.POST, "/api/auth/login", """
            {"username": "%s", "password": "%s"}
            """.formatted(user.username(), PASSWORD), null)
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("TOO_MANY_LOGIN_ATTEMPTS"));
    }

    @Test
    void resendWithinAMinute_isThrottled() throws Exception {
        String phone = uniquePhone();
        send(phone, "SIGNUP");
        call(HttpMethod.POST, "/api/auth/phone/send", """
            {"phoneNumber": "%s", "purpose": "SIGNUP"}
            """.formatted(phone), null)
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("VERIFICATION_TOO_SOON"));
    }

    @Test
    void manyNumbersFromOneIp_areLimitedPerHour() throws Exception {
        // 번호를 바꿔 가며 문자를 보내게 하는 남용: 한 곳(IP)에서는 시간당 10번까지만 보낼 수 있다
        String ip = "203.0.113." + java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 255);
        for (int i = 0; i < 10; i++) {
            callFrom(ip, HttpMethod.POST, "/api/auth/phone/send", """
                {"phoneNumber": "%s", "purpose": "SIGNUP"}
                """.formatted(uniquePhone()), null)
                .andExpect(status().isOk());
        }
        callFrom(ip, HttpMethod.POST, "/api/auth/phone/send", """
            {"phoneNumber": "%s", "purpose": "SIGNUP"}
            """.formatted(uniquePhone()), null)
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.code").value("VERIFICATION_IP_LIMIT"));
        // 프록시를 거쳐도 맨 앞의 원래 주소로 센다
        callFrom(ip + ", 74.220.52.132", HttpMethod.POST, "/api/auth/phone/send", """
            {"phoneNumber": "%s", "purpose": "SIGNUP"}
            """.formatted(uniquePhone()), null)
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void findUsernameAndResetPassword_byPhone() throws Exception {
        Login user = signup("USER");

        call(HttpMethod.POST, "/api/auth/find-username", """
            {"phoneNumber": "%s", "verificationToken": "%s"}
            """.formatted(user.phone(), verifiedToken(user.phone(), "FIND_USERNAME")), null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(user.username()));

        // 가입용 인증 토큰으로는 비밀번호를 바꿀 수 없다 (목적이 다르면 거절)
        String resetToken = verifiedToken(user.phone(), "RESET_PASSWORD");
        call(HttpMethod.POST, "/api/auth/reset-password", """
            {"username": "%s", "phoneNumber": "%s", "verificationToken": "%s", "newPassword": "brandNew123"}
            """.formatted(user.username(), user.phone(), resetToken), null)
            .andExpect(status().isOk());

        call(HttpMethod.POST, "/api/auth/login", """
            {"username": "%s", "password": "brandNew123"}
            """.formatted(user.username()), null)
            .andExpect(status().isOk());
        // 비밀번호를 바꾸면 예전 로그인 유지 토큰은 더 쓸 수 없다
        call(HttpMethod.POST, "/api/auth/refresh", """
            {"refreshToken": "%s"}
            """.formatted(user.refreshToken()), null)
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_rotates_andReuseRevokesEverything() throws Exception {
        Login user = signup("USER");

        String refreshed = call(HttpMethod.POST, "/api/auth/refresh", """
            {"refreshToken": "%s"}
            """.formatted(user.refreshToken()), null)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String newRefresh = JsonPath.read(refreshed, "$.refreshToken");
        String newAccess = JsonPath.read(refreshed, "$.accessToken");
        call(HttpMethod.GET, "/api/members/me", null, newAccess).andExpect(status().isOk());

        // 이미 바꿔서 버린 토큰이 다시 오면 탈취로 보고, 새 토큰까지 모두 끊는다
        call(HttpMethod.POST, "/api/auth/refresh", """
            {"refreshToken": "%s"}
            """.formatted(user.refreshToken()), null)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        call(HttpMethod.POST, "/api/auth/refresh", """
            {"refreshToken": "%s"}
            """.formatted(newRefresh), null)
            .andExpect(status().isUnauthorized());
    }

    @Test
    void changePhone_requiresVerificationOfTheNewNumber() throws Exception {
        Login user = signup("USER");
        String newPhone = uniquePhone();

        call(HttpMethod.PATCH, "/api/members/me/phone", """
            {"phoneNumber": "%s", "verificationToken": "%s"}
            """.formatted(newPhone, verifiedToken(newPhone, "CHANGE_PHONE")), user.accessToken())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phoneNumber").value(newPhone.substring(0, 3) + "-" + newPhone.substring(3, 7) + "-" + newPhone.substring(7)));
    }
}
