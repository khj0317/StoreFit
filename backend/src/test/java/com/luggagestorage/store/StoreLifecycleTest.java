package com.luggagestorage.store;

import com.luggagestorage.notification.NotificationType;
import com.luggagestorage.store.service.StoreLifecycleService;
import com.luggagestorage.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 시간이 지나면 저절로 바뀌는 예약 상태: 미결제 자동 취소, 노쇼, 연체 */
class StoreLifecycleTest extends IntegrationTest {

    @Autowired
    private StoreLifecycleService lifecycleService;

    private void moveDeadlineToPast(long storeId) {
        jdbcTemplate.update("update stores set payment_deadline = now() - interval '1 minute' where id = ?", storeId);
    }

    private List<String> notificationTypesOf(String phone) {
        return jdbcTemplate.queryForList("select type from notifications where phone_number = ? order by id", String.class, phone);
    }

    @Test
    void unpaidReservation_expiresAfterDeadline_andFreesCapacity() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 1);
        Login user = signup("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        long storeId = reservedId(user.accessToken(), placeId, 1, tomorrow, tomorrow);
        call(HttpMethod.GET, "/api/stores/" + storeId, null, user.accessToken())
            .andExpect(jsonPath("$.paymentDeadline").exists());
        // 결제 안 한 예약이 자리를 잡고 있어서 다른 예약은 막힌다
        reserve(user.accessToken(), placeId, 1, tomorrow, tomorrow).andExpect(status().isConflict());

        moveDeadlineToPast(storeId);
        assertThat(lifecycleService.expireUnpaidReservations()).isGreaterThanOrEqualTo(1);

        call(HttpMethod.GET, "/api/stores/" + storeId, null, user.accessToken())
            .andExpect(jsonPath("$.status").value("EXPIRED"));
        reserve(user.accessToken(), placeId, 1, tomorrow, tomorrow).andExpect(status().isCreated());
        assertThat(notificationTypesOf(user.phone())).contains(NotificationType.PAYMENT_EXPIRED.name());
    }

    @Test
    void expiredReservation_cannotBePaid_andPaymentApiIsNeverCalled() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        Login user = signup("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long storeId = reservedId(user.accessToken(), placeId, 1, tomorrow, tomorrow);

        String ready = call(HttpMethod.POST, "/api/payments/" + storeId + "/ready", null, user.accessToken())
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String orderId = com.jayway.jsonpath.JsonPath.read(ready, "$.orderId");

        moveDeadlineToPast(storeId);
        lifecycleService.expireUnpaidReservations();

        call(HttpMethod.POST, "/api/payments/confirm", """
            {"paymentKey": "pk", "orderId": "%s", "amount": 3000}
            """.formatted(orderId), user.accessToken())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PAYMENT_DEADLINE_PASSED"));
        verify(tossPaymentsClient, never()).confirm(anyString(), anyString(), anyInt());
        // 결제 내역에는 "결제 대기"로 남지 않고 실패(시간 초과)로 정리된다
        call(HttpMethod.GET, "/api/payments/me", null, user.accessToken())
            .andExpect(jsonPath("$[0].status").value("FAILED"));
    }

    @Test
    void paidReservation_isNeverExpired() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        Login user = signup("USER");
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long storeId = reservedId(user.accessToken(), placeId, 1, tomorrow, tomorrow);
        pay(user.accessToken(), storeId);

        jdbcTemplate.update("update stores set payment_deadline = now() - interval '1 minute' where id = ?", storeId);
        lifecycleService.expireUnpaidReservations();

        call(HttpMethod.GET, "/api/stores/" + storeId, null, user.accessToken())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void paidButNeverCheckedIn_becomesNoShow() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        Login user = signup("USER");
        LocalDate today = LocalDate.now();
        long storeId = reservedId(user.accessToken(), placeId, 1, today, today.plusDays(2));
        String code = pay(user.accessToken(), storeId);

        // 시작일이 어제였던 것처럼 옮긴다
        jdbcTemplate.update("update stores set start_date = start_date - 1, end_date = end_date - 1 where id = ?", storeId);
        lifecycleService.markNoShows();

        call(HttpMethod.GET, "/api/stores/" + storeId, null, user.accessToken())
            .andExpect(jsonPath("$.status").value("NO_SHOW"));
        // 노쇼 처리된 예약은 체크인할 수 없다
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner)
            .andExpect(status().isBadRequest());
        assertThat(notificationTypesOf(user.phone())).contains(NotificationType.NO_SHOW.name());
    }

    @Test
    void overdueStorage_showsFeeToOwner_andSendsNotice() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        Login user = signup("USER");
        LocalDate today = LocalDate.now();
        long storeId = reservedId(user.accessToken(), placeId, 2, today, today);
        String code = pay(user.accessToken(), storeId);
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner).andExpect(status().isOk());

        // 찾는 날이 사흘 전이었던 것처럼 옮긴다 → 3일 × LIGHT 3,000원 × 2개 = 18,000원
        jdbcTemplate.update("update stores set start_date = start_date - 3, end_date = end_date - 3 where id = ?", storeId);
        lifecycleService.sendOverdueNotices();

        call(HttpMethod.GET, "/api/owner/check-ins/" + code, null, owner)
            .andExpect(jsonPath("$.overdueDays").value(3))
            .andExpect(jsonPath("$.overdueFee").value(18000));
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-out", null, owner)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overdueFee").value(18000));
        assertThat(notificationTypesOf(user.phone())).contains(NotificationType.OVERDUE.name());
    }

    @Test
    void reservationJourney_sendsNotificationsToUserAndOwner() throws Exception {
        Login owner = signup("OWNER");
        long placeId = createPlace(owner.accessToken(), 5);
        Login user = signup("USER");
        LocalDate today = LocalDate.now();
        String code = pay(user.accessToken(), reservedId(user.accessToken(), placeId, 1, today, today));
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-in", null, owner.accessToken()).andExpect(status().isOk());
        call(HttpMethod.POST, "/api/owner/check-ins/" + code + "/check-out", null, owner.accessToken()).andExpect(status().isOk());

        assertThat(notificationTypesOf(user.phone())).containsSubsequence(
            NotificationType.PAYMENT_DONE.name(), NotificationType.CHECKED_IN.name(), NotificationType.CHECKED_OUT.name());
        assertThat(notificationTypesOf(owner.phone())).contains(
            NotificationType.BRANCH_APPROVED.name(), NotificationType.NEW_RESERVATION.name());
    }
}
