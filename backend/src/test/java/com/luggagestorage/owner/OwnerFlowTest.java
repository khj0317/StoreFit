package com.luggagestorage.owner;

import com.luggagestorage.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 예약 → 결제 → 운영자 QR 체크인 → 체크아웃 전체 흐름과 역할별 접근 제어 */
class OwnerFlowTest extends IntegrationTest {

    @Test
    void user_cannotUseOwnerApi_andIsNotLoggedOut() throws Exception {
        String user = signupAndLogin("USER");

        // 403이어도 INVALID_TOKEN이 아니어야 프론트가 로그아웃시키지 않는다
        call(HttpMethod.GET, "/api/owner/dashboard", null, user)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void signupAsAdmin_isRejected() throws Exception {
        String phone = uniquePhone();
        call(HttpMethod.POST, "/api/auth/signup", """
            {"username": "adminwannabe", "password": "password123", "name": "관리자",
             "phoneNumber": "%s", "verificationToken": "%s", "role": "ADMIN"}
            """.formatted(phone, verifiedToken(phone, "SIGNUP")), null)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_ROLE"));
    }

    @Test
    void fullFlow_reservePayCheckInCheckOut() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 10);
        String user = signupAndLogin("USER");
        LocalDate today = LocalDate.now();

        long storeId = reservedId(user, placeId, 2, today, today.plusDays(1));

        // 결제 전에는 체크인 코드가 없다
        call(HttpMethod.GET, "/api/stores/" + storeId, null, user)
            .andExpect(jsonPath("$.checkInCode").doesNotExist());

        String code = pay(user, storeId);

        // QR 스캔 → 예약 확인 (소문자·하이픈으로 직접 입력해도 같은 코드)
        String typed = code.substring(0, 4).toLowerCase() + "-" + code.substring(4);
        call(HttpMethod.GET, "/api/owner/check-ins/" + typed, null, owner)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nextAction").value("CHECK_IN"))
            .andExpect(jsonPath("$.paid").value(true));

        call(HttpMethod.GET, "/api/owner/dashboard", null, owner)
            .andExpect(jsonPath("$.arrivals[*].id", hasItem((int) storeId)));

        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_USE"))
            .andExpect(jsonPath("$.nextAction").value("CHECK_OUT"));

        // 같은 QR을 두 번 찍어도 두 번 체크인되지 않는다
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_STORE_STATUS"));

        // 보관 중인 예약은 이용자가 취소할 수 없다
        call(HttpMethod.POST, "/api/stores/" + storeId + "/cancel", null, user)
            .andExpect(status().isBadRequest());

        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-out", null, owner)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));

        call(HttpMethod.GET, "/api/stores/" + storeId, null, user)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.checkedInAt").exists())
            .andExpect(jsonPath("$.checkedOutAt").exists());
    }

    @Test
    void otherOwner_cannotSeeOrCheckInReservation() throws Exception {
        String owner = signupAndLogin("OWNER");
        String otherOwner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 10);
        String user = signupAndLogin("USER");
        LocalDate today = LocalDate.now();

        String code = pay(user, reservedId(user, placeId, 1, today, today));

        call(HttpMethod.GET, "/api/owner/check-ins/" + code, null, otherOwner)
            .andExpect(status().isNotFound());
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, otherOwner)
            .andExpect(status().isNotFound());
    }

    @Test
    void checkIn_beforeStartDate_isRejected() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 10);
        String user = signupAndLogin("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        String code = pay(user, reservedId(user, placeId, 1, tomorrow, tomorrow));

        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CHECK_IN_DATE_INVALID"));
    }

    @Test
    void cancelPaidReservation_refundsAndFreesCapacity() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 1);
        String user = signupAndLogin("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        allowTossCancel();

        long storeId = reservedId(user, placeId, 1, tomorrow, tomorrow);
        pay(user, storeId);
        reserve(user, placeId, 1, tomorrow, tomorrow).andExpect(status().isConflict());

        call(HttpMethod.GET, "/api/stores/" + storeId + "/refund-preview", null, user)
            .andExpect(jsonPath("$.refundRate").value(100))
            .andExpect(jsonPath("$.refundAmount").value(3000));

        call(HttpMethod.POST, "/api/stores/" + storeId + "/cancel", null, user)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELED"))
            .andExpect(jsonPath("$.refundedAmount").value(3000));
        verify(tossPaymentsClient).cancel(anyString(), anyString(), eq(3000), anyString());

        // 취소한 예약은 자리를 차지하지 않는다
        reserve(user, placeId, 1, tomorrow, tomorrow).andExpect(status().isCreated());
    }

    @Test
    void cancellingTwiceAtTheSameTime_refundsOnlyOnce() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        String user = signupAndLogin("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        allowTossCancel();
        long storeId = reservedId(user, placeId, 1, tomorrow, tomorrow);
        pay(user, storeId);

        List<Integer> statuses = runConcurrently(3, index ->
            call(HttpMethod.POST, "/api/stores/" + storeId + "/cancel", null, user).andReturn().getResponse().getStatus());

        assertThat(statuses).filteredOn(code -> code == 200).hasSize(1);
        verify(tossPaymentsClient, org.mockito.Mockito.times(1)).cancel(anyString(), anyString(), eq(3000), anyString());
    }

    @Test
    void ownerCannotShrinkCapacityBelowExistingReservations() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        String user = signupAndLogin("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        reservedId(user, placeId, 3, tomorrow, tomorrow);

        call(HttpMethod.PUT, "/api/owner/places/" + placeId, """
            {"capacity": 2}
            """, owner)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PLACE_CAPACITY_TOO_SMALL"));

        call(HttpMethod.PUT, "/api/owner/places/" + placeId, """
            {"capacity": 3, "description": "운영 시간 10시~22시"}
            """, owner)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.capacity").value(3))
            .andExpect(jsonPath("$.description").value("운영 시간 10시~22시"));
    }

    // ───────── 정식 지점 ─────────

    @Test
    void ownerUpdate_cannotChangeOfficialNameOrAddress() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        String officialName = storagePlaceRepository.findById(placeId).orElseThrow().getName();

        call(HttpMethod.PUT, "/api/owner/places/" + placeId, """
            {"name": "내 마음대로 지점", "address": "아무 데나", "capacity": 5}
            """, owner)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value(officialName))
            .andExpect(jsonPath("$.address").value("서울 어딘가"));
    }

    @Test
    void applyingDoesNotGiveTheBranchUntilAdminApproves() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = registerBranch();

        long applicationId = appliedId(owner, placeId, 8);
        call(HttpMethod.GET, "/api/owner/applications", null, owner)
            .andExpect(jsonPath("$[0].status").value("PENDING"));
        // 승인 전에는 운영자의 지점도 아니고, 이용자에게도 보이지 않는다
        call(HttpMethod.GET, "/api/owner/places", null, owner)
            .andExpect(jsonPath("$[*].id", not(hasItem((int) placeId))));

        // 같은 지점에 심사 중인 신청을 또 낼 수 없다
        apply(owner, placeId, 8)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("BRANCH_APPLICATION_DUPLICATE"));

        call(HttpMethod.POST, "/api/admin/applications/" + applicationId + "/approve", null, adminToken())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
        call(HttpMethod.GET, "/api/owner/places", null, owner)
            .andExpect(jsonPath("$[*].id", hasItem((int) placeId)));
    }

    @Test
    void approvingOneApplication_rejectsTheOthersForThatBranch() throws Exception {
        String first = signupAndLogin("OWNER");
        String second = signupAndLogin("OWNER");
        long placeId = registerBranch();
        long firstApplication = appliedId(first, placeId, 8);
        long secondApplication = appliedId(second, placeId, 8);
        String admin = adminToken();

        call(HttpMethod.POST, "/api/admin/applications/" + firstApplication + "/approve", null, admin)
            .andExpect(status().isOk());

        call(HttpMethod.GET, "/api/owner/applications", null, second)
            .andExpect(jsonPath("$[0].status").value("REJECTED"))
            .andExpect(jsonPath("$[0].rejectReason").exists());
        call(HttpMethod.POST, "/api/admin/applications/" + secondApplication + "/approve", null, admin)
            .andExpect(status().isConflict());
        // 이미 운영자가 있는 지점에는 새로 신청할 수 없다
        apply(second, placeId, 8)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("BRANCH_ALREADY_TAKEN"));
    }

    @Test
    void concurrentApprovals_onlyOneOwnerGetsTheBranch() throws Exception {
        long placeId = registerBranch();
        List<Long> applications = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            applications.add(appliedId(signupAndLogin("OWNER"), placeId, 5));
        }
        String admin = adminToken();

        List<Integer> statuses = runConcurrently(applications.size(), index ->
            call(HttpMethod.POST, "/api/admin/applications/" + applications.get(index) + "/approve", null, admin)
                .andReturn().getResponse().getStatus());

        assertThat(statuses).filteredOn(code -> code == 200).hasSize(1);
        assertThat(statuses).filteredOn(code -> code == 409).hasSize(4);
    }

    @Test
    void ownerAndUser_cannotUseAdminApi() throws Exception {
        call(HttpMethod.GET, "/api/admin/applications", null, signupAndLogin("OWNER"))
            .andExpect(status().isForbidden());
        call(HttpMethod.GET, "/api/admin/branches", null, signupAndLogin("USER"))
            .andExpect(status().isForbidden());
    }

    @Test
    void unclaimedBranch_isHiddenFromUsersAndCannotBeReserved() throws Exception {
        long placeId = registerBranch();
        String user = signupAndLogin("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        call(HttpMethod.GET, "/api/places", null, null)
            .andExpect(jsonPath("$[*].id", not(hasItem((int) placeId))));
        reserve(user, placeId, 1, tomorrow, tomorrow)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }
}
