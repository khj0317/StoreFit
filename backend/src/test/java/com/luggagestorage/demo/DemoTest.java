package com.luggagestorage.demo;

import com.jayway.jsonpath.JsonPath;
import com.luggagestorage.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    void resetApi_isOffUnlessAllowed() throws Exception {
        // E2E 테스트용 되돌리기 API는 배포 설정(allow-reset: false)에서는 없는 API처럼 막힌다
        call(HttpMethod.POST, "/api/auth/demo-reset", null, null)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("DEMO_DISABLED"));
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

        // 비밀번호 변경·탈퇴도 "비밀번호가 틀렸다"가 아니라 체험 계정이라 막혔다고 알려 준다
        call(HttpMethod.POST, "/api/members/me/password", """
            {"currentPassword": "anything", "newPassword": "newpassword1"}
            """, user)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMO_ACCOUNT_RESTRICTED"));
        call(HttpMethod.DELETE, "/api/members/me", """
            {"password": "anything"}
            """, user)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMO_ACCOUNT_RESTRICTED"));
    }

    @Test
    void demoOwner_cannotChangeBranchDescription_andCapacityIsRestoredOnReset() throws Exception {
        String owner = demoLogin("OWNER");
        var gangnam = storagePlaceRepository.findByCode("GANGNAM").orElseThrow();

        // 정식 지점 소개는 모든 이용자에게 보이므로 바꿀 수 없다
        call(HttpMethod.PUT, "/api/owner/places/" + gangnam.getId(), """
            {"capacity": 3, "description": "아무나 쓴 소개"}
            """, owner)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMO_ACCOUNT_RESTRICTED"));

        // 수용량은 바꿔 볼 수 있지만 되돌리면 처음 값으로 돌아간다
        call(HttpMethod.PUT, "/api/owner/places/" + gangnam.getId(), """
            {"capacity": 500, "description": %s}
            """.formatted(gangnam.getDescription() == null ? "null" : "\"" + gangnam.getDescription() + "\""), owner)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.capacity").value(500));
        demoService.reset();
        assertThat(storagePlaceRepository.findByCode("GANGNAM").orElseThrow().getCapacity()).isEqualTo(3);
    }

    @Test
    void demoOwner_cannotApplyForBranch_andLeftoverApplicationsAreClearedOnReset() throws Exception {
        String owner = demoLogin("OWNER");
        long placeId = registerBranch();
        apply(owner, placeId, 5)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMO_ACCOUNT_RESTRICTED"));

        // 막기 전에 들어간 신청은 되돌릴 때 관리자 심사 목록에서 지운다
        Long ownerId = memberRepository.findByUsername(DemoAccounts.OWNER_USERNAME).orElseThrow().getId();
        jdbcTemplate.update("""
            insert into branch_applications (created_at, updated_at, place_id, applicant_id, capacity, status)
            values (now(), now(), ?, ?, 5, 'PENDING')
            """, placeId, ownerId);
        demoService.reset();
        Integer pending = jdbcTemplate.queryForObject(
            "select count(*) from branch_applications where applicant_id = ? and status = 'PENDING'", Integer.class, ownerId);
        assertThat(pending).isZero();
    }

    @Test
    void usedDropOff_isRestoredAfterAWhile_soTheNextVisitorCanTryQrCheckIn() throws Exception {
        DemoService target = AopTestUtils.getTargetObject(demoService);
        ReflectionTestUtils.setField(target, "dropOffUsedSince", null);
        // 아직 아무도 체크인하지 않았으면 되돌리지 않는다
        assertThat(demoService.refreshIfDropOffUsed()).isFalse();

        String user = demoLogin("USER");
        String owner = demoLogin("OWNER");
        String body = call(HttpMethod.GET, "/api/stores/me", null, user).andReturn().getResponse().getContentAsString();
        String code = JsonPath.<List<String>>read(body, "$[?(@.status == 'PENDING')].checkInCode").get(0);
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner).andExpect(status().isOk());

        // 체크인한 사람이 결과를 볼 시간은 남긴다
        assertThat(demoService.refreshIfDropOffUsed()).isFalse();
        assertThat(demoService.refreshIfDropOffUsed()).isFalse();
        assertThat(statusesOfDemoUser(user)).containsExactlyInAnyOrder("IN_USE", "IN_USE", "COMPLETED");

        // 그 상태로 시간이 지나면 처음 상태로 되돌려 다음 방문자도 QR 체크인을 해볼 수 있다
        ReflectionTestUtils.setField(target, "dropOffUsedSince",
            Instant.now().minus(DemoService.REFRESH_AFTER_USED.plusMinutes(1)));
        assertThat(demoService.refreshIfDropOffUsed()).isTrue();
        assertThat(statusesOfDemoUser(user)).containsExactlyInAnyOrder("IN_USE", "PENDING", "COMPLETED");
    }

    @Test
    void demoUser_cannotUploadImages() throws Exception {
        String user = demoLogin("USER");
        mockMvc.perform(multipart("/api/uploads/images")
                .file(new MockMultipartFile("files", "a.png", "image/png", new byte[]{1, 2, 3}))
                .header("Authorization", "Bearer " + user))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("DEMO_ACCOUNT_RESTRICTED"));
    }
}
