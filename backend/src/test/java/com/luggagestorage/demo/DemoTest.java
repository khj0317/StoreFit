package com.luggagestorage.demo;

import com.jayway.jsonpath.JsonPath;
import com.luggagestorage.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 로그인 화면의 "둘러보기": 휴대폰 인증 없이 체험 이용자·사장님으로 둘러본다 */
class DemoTest extends IntegrationTest {

    @Autowired
    private DemoService demoService;

    @BeforeEach
    void resetDemoData() {
        demoService.reset();
    }

    private String demoLogin(String role) throws Exception {
        String body = call(HttpMethod.POST, "/api/auth/demo-login", """
            {"role": "%s"}
            """.formatted(role), null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.demo").value(true))
            .andExpect(jsonPath("$.role").value(role))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private List<String> statusesOfDemoUser(String userToken) throws Exception {
        String body = call(HttpMethod.GET, "/api/stores/me", null, userToken)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].status");
    }

    @Test
    void demoUser_startsWithStoredLuggage_todaysDropOff_andHistory() throws Exception {
        String user = demoLogin("USER");
        assertThat(statusesOfDemoUser(user)).containsExactlyInAnyOrder("IN_USE", "PENDING", "COMPLETED");

        // 체험 사장님은 정식 지점을 운영하고 있어서 이용자 화면에 예약할 지점이 보인다
        String places = call(HttpMethod.GET, "/api/places", null, user).andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(places, "$[*].name")).anyMatch(name -> name.contains("홍대"));
    }

    @Test
    void demoOwner_canCheckInTodaysReservation_withoutSendingRealTexts() throws Exception {
        String user = demoLogin("USER");
        String owner = demoLogin("OWNER");
        String body = call(HttpMethod.GET, "/api/stores/me", null, user).andReturn().getResponse().getContentAsString();
        String code = JsonPath.<List<String>>read(body, "$[?(@.status == 'PENDING')].checkInCode").get(0);

        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner)
            .andExpect(status().isOk());

        // 체험 계정의 번호는 실제로 없는 번호라 알림 기록도 만들지 않는다
        Integer sent = jdbcTemplate.queryForObject(
            "select count(*) from notifications where phone_number in (?, ?)", Integer.class,
            DemoAccounts.USER_PHONE, DemoAccounts.OWNER_PHONE);
        assertThat(sent).isZero();
    }

    @Test
    void demoPaymentRefund_doesNotCallThePaymentCompany() throws Exception {
        String user = demoLogin("USER");
        String body = call(HttpMethod.GET, "/api/stores/me", null, user).andReturn().getResponse().getContentAsString();
        Number storeId = JsonPath.<List<Number>>read(body, "$[?(@.status == 'PENDING')].id").get(0);

        call(HttpMethod.POST, "/api/stores/" + storeId + "/cancel", null, user)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELED"));
        verify(tossPaymentsClient, never()).cancel(anyString(), anyString(), anyInt(), anyString());

        // 다음 날 처음 상태로 되돌리면 다시 세 건이 된다
        demoService.reset();
        assertThat(statusesOfDemoUser(user)).containsExactlyInAnyOrder("IN_USE", "PENDING", "COMPLETED");
    }

    @Test
    void demoAccounts_cannotChangeSharedProfile_orLoginWithPassword() throws Exception {
        String user = demoLogin("USER");
        call(HttpMethod.PATCH, "/api/members/me", """
            {"name": "바꾼 이름"}
            """, user)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMO_ACCOUNT_RESTRICTED"));

        call(HttpMethod.POST, "/api/auth/login", """
            {"username": "demo_owner", "password": "demo1234!"}
            """, null)
            .andExpect(status().isUnauthorized());
        call(HttpMethod.POST, "/api/auth/demo-login", """
            {"role": "ADMIN"}
            """, null)
            .andExpect(status().isBadRequest());
    }
}
