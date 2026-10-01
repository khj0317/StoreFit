package com.luggagestorage.store;

import com.jayway.jsonpath.JsonPath;
import com.luggagestorage.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수용량 5인 보관소에 20명이 동시에 예약해도 정확히 5건만 받아야 한다.
 * 보관소 행을 SELECT ... FOR UPDATE로 잠그지 않으면 여러 요청이 같은 "남은 자리"를 보고
 * 동시에 저장해서 5건을 넘게 된다 (락을 빼고 돌리면 이 테스트가 실패한다).
 */
class ReservationConcurrencyTest extends IntegrationTest {

    @Test
    void concurrentReservations_neverExceedCapacity() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 5);
        LocalDate day = LocalDate.now().plusDays(1);

        int users = 20;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            tokens.add(signupAndLogin("USER"));
        }

        List<Integer> statuses = runConcurrently(users, index ->
            reserve(tokens.get(index), placeId, 1, day, day).andReturn().getResponse().getStatus());

        assertThat(statuses).filteredOn(code -> code == 201).hasSize(5);
        assertThat(statuses).filteredOn(code -> code == 409).hasSize(users - 5);

        // 서버가 계산한 남은 자리도 0이어야 한다
        String places = call(HttpMethod.GET, "/api/places?startDate=" + day + "&endDate=" + day, null, null)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        List<Integer> remaining = JsonPath.read(places, "$[?(@.id == " + placeId + ")].remaining");
        assertThat(remaining).containsExactly(0);
    }

    @Test
    void capacityIsCountedPerDay_soNonOverlappingReservationsFit() throws Exception {
        String owner = signupAndLogin("OWNER");
        long placeId = createPlace(owner, 2);
        String user = signupAndLogin("USER");
        LocalDate start = LocalDate.now().plusDays(1);

        // 1~2일차에 2개 → 꽉 참. 3일차는 비어 있으므로 다른 예약이 들어갈 수 있다
        reserve(user, placeId, 2, start, start.plusDays(1)).andExpect(status().isCreated());
        reserve(user, placeId, 1, start.plusDays(1), start.plusDays(2)).andExpect(status().isConflict());
        reserve(user, placeId, 2, start.plusDays(2), start.plusDays(3)).andExpect(status().isCreated());
    }
}
