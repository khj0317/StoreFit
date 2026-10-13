package com.luggagestorage.demo;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.entity.MemberRole;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
import com.luggagestorage.store.repository.StoreRepository;
import com.luggagestorage.store.service.CheckInCodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 체험 계정과 체험 데이터. 포트폴리오를 보는 사람이 휴대폰 인증 없이 이용자·사장님 화면을 둘러볼 수 있게 한다.
 * - 체험 사장님: 정식 지점 세 곳(운영자가 없을 때만)을 운영한다
 * - 체험 이용자: 보관 중인 짐, 오늘 맡길 짐(체크인 QR), 지난 보관 내역을 갖고 시작한다
 * 둘러보는 사람이 데이터를 바꿔도 하루에 한 번(그날 처음 깨어났을 때) 처음 상태로 되돌린다.
 */
@Slf4j
@Service
public class DemoService {

    private static final String RESET_JOB = "DEMO_RESET";
    /**
     * 오늘 맡길 짐(체크인 QR)은 하나뿐이라, 누가 체크인하거나 취소하면 다음 방문자는 QR 체크인을 해볼 수 없다.
     * 그런 상태가 이만큼 이어지면 처음 상태로 되돌린다 (방금 체험한 사람이 결과를 볼 시간은 남긴다)
     */
    static final Duration REFRESH_AFTER_USED = Duration.ofMinutes(20);

    /** 체험 사장님이 맡을 지점과 수용량 (강남역점은 자리 마감을 시험해보기 좋게 작게) */
    private static final Map<String, Integer> BRANCHES = new LinkedHashMap<>();

    static {
        BRANCHES.put("HONGDAE", 20);
        BRANCHES.put("SINCHON", 10);
        BRANCHES.put("GANGNAM", 3);
    }

    private final boolean enabled;
    /** 개발·E2E 테스트에서만 켠다: 체험 데이터를 아무 때나 처음 상태로 되돌리는 API (배포에서는 꺼짐) */
    private final boolean allowReset;
    private final MemberRepository memberRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final StoreRepository storeRepository;
    private final PaymentRepository paymentRepository;
    private final CheckInCodeGenerator checkInCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    /** 오늘 맡길 짐이 쓰인 것을 처음 알아챈 시각 (서버가 한 대라 메모리에 둔다. 재시작하면 다시 센다) */
    private Instant dropOffUsedSince;

    public DemoService(@Value("${app.demo.enabled:false}") boolean enabled,
                       @Value("${app.demo.allow-reset:false}") boolean allowReset,
                       MemberRepository memberRepository, StoragePlaceRepository storagePlaceRepository,
                       StoreRepository storeRepository, PaymentRepository paymentRepository,
                       CheckInCodeGenerator checkInCodeGenerator, PasswordEncoder passwordEncoder,
                       JdbcTemplate jdbcTemplate, Clock clock) {
        this.enabled = enabled;
        this.allowReset = allowReset;
        this.memberRepository = memberRepository;
        this.storagePlaceRepository = storagePlaceRepository;
        this.storeRepository = storeRepository;
        this.paymentRepository = paymentRepository;
        this.checkInCodeGenerator = checkInCodeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    /** 체험하기 버튼: 체험 계정을 돌려준다 (토큰 발급은 AuthController가 한다) */
    @Transactional(readOnly = true)
    public Member demoMember(MemberRole role) {
        if (!enabled) {
            throw new BusinessException(ErrorCode.DEMO_DISABLED);
        }
        if (role != MemberRole.USER && role != MemberRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        return memberRepository.findByUsername(role == MemberRole.OWNER ? DemoAccounts.OWNER_USERNAME : DemoAccounts.USER_USERNAME)
            .orElseThrow(() -> new BusinessException(ErrorCode.DEMO_DISABLED));
    }

    /** 서버가 켜질 때: 체험 계정을 만들고 비밀번호를 아무도 모르는 값으로 바꾼다 */
    @Transactional
    public void prepareAccounts() {
        if (!enabled) {
            return;
        }
        // 화면에서 이름 뒤에 "님"을 붙이므로 이름에는 "님"을 넣지 않는다
        Member owner = ensureMember(DemoAccounts.OWNER_USERNAME, "체험 사장", DemoAccounts.OWNER_PHONE, MemberRole.OWNER);
        ensureMember(DemoAccounts.USER_USERNAME, "체험 이용자", DemoAccounts.USER_PHONE, MemberRole.USER);
        BRANCHES.forEach((code, capacity) -> storagePlaceRepository.findByCode(code)
            .filter(place -> !place.isOperating())
            .ifPresent(place -> place.claim(owner, capacity, null)));
    }

    /**
     * 오늘 아직 안 했으면 체험 데이터를 처음 상태로 되돌린다. 서버가 잠들었다 깨어나도 하루에 한 번만 한다.
     * @return 이번에 되돌렸으면 true
     */
    @Transactional
    public boolean resetOncePerDay() {
        if (!enabled) {
            return false;
        }
        int claimed = jdbcTemplate.update(
            "insert into daily_job_runs (job_name, run_date) values (?, ?) on conflict do nothing", RESET_JOB, LocalDate.now(clock));
        if (claimed == 0) {
            return false;
        }
        reset();
        return true;
    }

    /**
     * 오늘 맡길 짐이 체크인·취소되어 QR 체크인을 해볼 수 없는 상태가 REFRESH_AFTER_USED 넘게 이어지면 처음 상태로 되돌린다.
     * 스케줄러가 10분마다 부른다.
     * @return 이번에 되돌렸으면 true
     */
    @Transactional
    public synchronized boolean refreshIfDropOffUsed() {
        if (!enabled) {
            return false;
        }
        Instant now = clock.instant();
        if (hasDropOffWaiting()) {
            dropOffUsedSince = null;
            return false;
        }
        if (dropOffUsedSince == null) {
            dropOffUsedSince = now;
            return false;
        }
        if (Duration.between(dropOffUsedSince, now).compareTo(REFRESH_AFTER_USED) < 0) {
            return false;
        }
        reset();
        dropOffUsedSince = null;
        return true;
    }

    /** 체험 이용자에게 결제를 마치고 오늘 체크인을 기다리는 예약이 있는지 */
    private boolean hasDropOffWaiting() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            select exists (
                select 1 from stores s
                join members m on m.id = s.member_id
                join payments p on p.store_id = s.id
                where m.username = ? and s.status = 'PENDING' and s.start_date = ? and p.status = 'DONE')
            """, Boolean.class, DemoAccounts.USER_USERNAME, LocalDate.now(clock)));
    }

    /** E2E 테스트 시작 전에 체험 데이터를 처음 상태로 되돌린다. 배포에서는 꺼져 있어 없는 API처럼 응답한다 */
    @Transactional
    public void resetOnRequest() {
        if (!enabled || !allowReset) {
            throw new BusinessException(ErrorCode.DEMO_DISABLED);
        }
        reset();
    }

    /** 체험 이용자의 예약을 모두 지우고 처음 상태의 예약을 다시 만든다 */
    @Transactional
    public void reset() {
        if (!enabled) {
            return;
        }
        Member user = memberRepository.findByUsername(DemoAccounts.USER_USERNAME).orElse(null);
        Member owner = memberRepository.findByUsername(DemoAccounts.OWNER_USERNAME).orElse(null);
        if (user == null || owner == null) {
            return;
        }

        jdbcTemplate.update("delete from store_images where store_id in (select id from stores where member_id = ?)", user.getId());
        jdbcTemplate.update("delete from payments where store_id in (select id from stores where member_id = ?)", user.getId());
        jdbcTemplate.update("delete from stores where member_id = ?", user.getId());
        // 예전에 체험 사장님으로 낸 운영 신청이 관리자 심사 목록에 남지 않게 지운다 (지금은 신청 자체를 막는다)
        jdbcTemplate.update("delete from branch_applications where applicant_id = ? and status = 'PENDING'", owner.getId());
        // 체험하기를 누를 때마다 쌓이는 로그인 기록 중 끝난 것을 정리한다
        jdbcTemplate.update("delete from refresh_tokens where member_id in (?, ?) and (revoked_at is not null or expires_at < now())",
            user.getId(), owner.getId());

        List<StoragePlace> places = storagePlaceRepository.findByOwnerOrderByIdAsc(owner);
        if (places.isEmpty()) {
            log.warn("체험 사장님이 운영하는 지점이 없어 체험 예약을 만들지 않았습니다.");
            return;
        }
        // 둘러보는 사람이 바꾼 수용량을 처음 값으로 되돌린다 (소개는 OwnerService가 바꾸지 못하게 막는다)
        places.forEach(place -> {
            Integer capacity = BRANCHES.get(place.getCode());
            if (capacity != null) {
                place.updateOperation(capacity, null);
            }
        });
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        // 보관 중: 그저께 맡겼고 내일 찾는 짐
        Store inUse = paidStore(user, placeAt(places, 0), "여름옷 정리 상자", "계절 지난 옷과 얇은 이불",
            StoreCategory.CLOTHES, 2, today.minusDays(2), today.plusDays(1), now.minusDays(3));
        inUse.checkIn(today.minusDays(2).atTime(10, 20));

        // 결제 완료: 오늘 맡길 짐 (체험 사장님 화면에서 QR 체크인을 해볼 수 있다)
        paidStore(user, placeAt(places, 1), "기숙사 이불 세트", "방학 동안 맡길 이불과 베개",
            StoreCategory.MEDIUM, 1, today, today.plusDays(3), now.minusHours(2));

        // 지난 보관 내역
        Store done = paidStore(user, placeAt(places, 2), "전공책 박스", "다음 학기에 쓸 책",
            StoreCategory.LIGHT, 3, today.minusDays(12), today.minusDays(9), now.minusDays(14));
        done.checkIn(today.minusDays(12).atTime(9, 40));
        done.checkOut(today.minusDays(9).atTime(18, 5));

        log.info("체험 데이터를 처음 상태로 되돌렸습니다.");
    }

    private static StoragePlace placeAt(List<StoragePlace> places, int index) {
        return places.get(Math.min(index, places.size() - 1));
    }

    private Store paidStore(Member user, StoragePlace place, String name, String description, StoreCategory category,
                            int count, LocalDate start, LocalDate end, LocalDateTime paidAt) {
        int price = (int) (category.getDailyRate() * count * (ChronoUnit.DAYS.between(start, end) + 1));
        Store store = storeRepository.save(new Store(user, place, name, description, category, count, start, end, price));
        store.issueCheckInCode(checkInCodeGenerator.generate());
        Payment payment = new Payment(store, "demo-" + UUID.randomUUID(), price);
        payment.approve(DemoAccounts.PAYMENT_KEY_PREFIX + UUID.randomUUID(), "카드", paidAt);
        paymentRepository.save(payment);
        return store;
    }

    private Member ensureMember(String username, String name, String phone, MemberRole role) {
        String unusablePassword = passwordEncoder.encode(HexFormat.of().formatHex(randomBytes()));
        return memberRepository.findByUsername(username)
            .map(member -> {
                member.changePassword(unusablePassword);
                member.rename(name);
                return member;
            })
            .orElseGet(() -> memberRepository.save(role == MemberRole.OWNER
                ? Member.createOwner(username, unusablePassword, name, null, phone)
                : Member.createUser(username, unusablePassword, name, null, phone)));
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return bytes;
    }
}
