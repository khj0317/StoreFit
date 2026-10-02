package com.luggagestorage.support;

import com.jayway.jsonpath.JsonPath;
import com.luggagestorage.payment.client.TossConfirmResult;
import com.luggagestorage.payment.client.TossPaymentsClient;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨테이너와 스프링 컨텍스트를 모든 테스트 클래스가 공유하므로,
 * 테스트끼리 데이터가 겹치지 않게 매번 고유한 아이디와 새 보관소를 만든다.
 * 실제 돈이 오가지 않도록 Toss 호출만 가짜로 바꾼다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected TossPaymentsClient tossPaymentsClient;

    @Autowired
    protected StoragePlaceRepository storagePlaceRepository;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    private static final AtomicLong PHONE_SEQUENCE = new AtomicLong(ThreadLocalRandom.current().nextLong(10_000_000L, 80_000_000L));

    /** 테스트끼리 겹치지 않는 휴대폰 번호 (010 + 8자리) */
    protected static String uniquePhone() {
        return "010" + PHONE_SEQUENCE.incrementAndGet();
    }

    /** 가입 화면과 같은 순서: 인증번호 받기 → 확인 → 토큰으로 가입 */
    protected String verifiedToken(String phone, String purpose) throws Exception {
        String sent = call(HttpMethod.POST, "/api/auth/phone/send", """
            {"phoneNumber": "%s", "purpose": "%s"}
            """.formatted(phone, purpose), null)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String code = JsonPath.read(sent, "$.devCode");
        String verified = call(HttpMethod.POST, "/api/auth/phone/verify", """
            {"phoneNumber": "%s", "purpose": "%s", "code": "%s"}
            """.formatted(phone, purpose, code), null)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(verified, "$.verificationToken");
    }

    protected record Login(String username, String phone, String accessToken, String refreshToken) {
    }

    protected Login signup(String role) throws Exception {
        String username = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String phone = uniquePhone();
        String token = verifiedToken(phone, "SIGNUP");
        call(HttpMethod.POST, "/api/auth/signup", """
            {"username": "%s", "password": "%s", "name": "테스트", "phoneNumber": "%s", "verificationToken": "%s", "role": "%s"}
            """.formatted(username, PASSWORD, phone, token, role), null)
            .andExpect(status().isCreated());
        return login(username, phone);
    }

    protected Login login(String username, String phone) throws Exception {
        String body = call(HttpMethod.POST, "/api/auth/login", """
            {"username": "%s", "password": "%s"}
            """.formatted(username, PASSWORD), null)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return new Login(username, phone, JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    /** 관리자는 가입으로 만들 수 없어서 DB에 바로 만든다 (배포에서는 AdminAccountInitializer가 만든다) */
    protected String adminToken() throws Exception {
        String username = "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        memberRepository.save(Member.createAdmin(username, passwordEncoder.encode(PASSWORD), "관리자"));
        return login(username, null).accessToken();
    }

    /**
     * 인증 문자는 IP별 발송 한도가 있어서, 테스트끼리 한도를 나눠 쓰지 않게 요청마다 다른 IP에서 온 것처럼 보낸다.
     * IP 한도 자체를 시험할 때는 callFrom으로 IP를 정한다.
     */
    protected ResultActions call(HttpMethod method, String url, String json, String accessToken) throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String ip = "10." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(1, 255);
        return callFrom(ip, method, url, json, accessToken);
    }

    protected ResultActions callFrom(String ip, HttpMethod method, String url, String json, String accessToken) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, url).header("X-Forwarded-For", ip);
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        return mockMvc.perform(builder);
    }

    protected String signupAndLogin(String role) throws Exception {
        return signup(role).accessToken();
    }

    /**
     * 본사가 정식 지점을 등록해 둔 것처럼 운영자 없는 지점을 하나 만든다.
     * 마이그레이션의 임시 지점은 개수가 정해져 있어서, 테스트마다 새 지점을 만들어 겹치지 않게 한다.
     */
    protected long registerBranch() {
        String code = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return storagePlaceRepository.save(
            StoragePlace.officialBranch(code, "스토어핏 테스트점 " + code, "서울 어딘가", "본사 소개", 10)).getId();
    }

    protected ResultActions apply(String ownerToken, long placeId, int capacity) throws Exception {
        return call(HttpMethod.POST, "/api/owner/branches/" + placeId + "/applications", """
            {"capacity": %d, "message": "잘 운영하겠습니다"}
            """.formatted(capacity), ownerToken);
    }

    protected long appliedId(String ownerToken, long placeId, int capacity) throws Exception {
        String body = apply(ownerToken, placeId, capacity)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** 정식 지점을 등록하고, 운영자가 신청한 뒤 관리자가 승인한 상태로 만든다 */
    protected long createPlace(String ownerToken, int capacity) throws Exception {
        long placeId = registerBranch();
        long applicationId = appliedId(ownerToken, placeId, capacity);
        call(HttpMethod.POST, "/api/admin/applications/" + applicationId + "/approve", null, adminToken())
            .andExpect(status().isOk());
        return placeId;
    }

    protected ResultActions reserve(String userToken, long placeId, int luggageCount, LocalDate start, LocalDate end) throws Exception {
        return call(HttpMethod.POST, "/api/stores", """
            {"placeId": %d, "name": "캐리어", "category": "LIGHT", "luggageCount": %d,
             "startDate": "%s", "endDate": "%s", "imageUrls": []}
            """.formatted(placeId, luggageCount, start, end), userToken);
    }

    protected long reservedId(String userToken, long placeId, int luggageCount, LocalDate start, LocalDate end) throws Exception {
        String body = reserve(userToken, placeId, luggageCount, start, end)
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** ready → (가짜) Toss 승인 → confirm 까지 진행하고 발급된 체크인 코드를 돌려준다 */
    protected String pay(String userToken, long storeId) throws Exception {
        String ready = call(HttpMethod.POST, "/api/payments/" + storeId + "/ready", null, userToken)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String orderId = JsonPath.read(ready, "$.orderId");
        int amount = JsonPath.read(ready, "$.amount");

        String paymentKey = "pk-" + orderId;
        when(tossPaymentsClient.confirm(paymentKey, orderId, amount)).thenReturn(new TossConfirmResult(
            paymentKey, orderId, amount, "카드", OffsetDateTime.now().toString(), "DONE"));

        call(HttpMethod.POST, "/api/payments/confirm", """
            {"paymentKey": "%s", "orderId": "%s", "amount": %d}
            """.formatted(paymentKey, orderId, amount), userToken)
            .andExpect(status().isOk());

        String store = call(HttpMethod.GET, "/api/stores/" + storeId, null, userToken)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(store, "$.checkInCode");
    }

    protected void allowTossCancel() {
        org.mockito.Mockito.doNothing().when(tossPaymentsClient).cancel(anyString(), anyString(), anyInt(), anyString());
    }

    protected interface IndexedTask<T> {
        T run(int index) throws Exception;
    }

    /** 모든 스레드가 준비된 뒤 한꺼번에 출발시켜 실제 경쟁 상황을 만든다 */
    protected static <T> List<T> runConcurrently(int threads, IndexedTask<T> task) throws Exception {
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                Callable<T> callable = () -> {
                    ready.countDown();
                    start.await();
                    return task.run(index);
                };
                futures.add(executor.submit(callable));
            }
            ready.await();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
