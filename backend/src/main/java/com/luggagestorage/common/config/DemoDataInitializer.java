package com.luggagestorage.common.config;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 로컬에서 바로 써볼 수 있도록 데모 계정을 만든다. (정식 지점 자체는 V6·V7 마이그레이션이 만든다)
 * - demo_owner: 정식 지점 세 곳을 운영 중인 사장님 (심사 없이 바로 맡긴다)
 * - demo_admin: 지점 등록·운영 신청 심사를 해볼 수 있는 관리자
 * app.demo-data.enabled=true 일 때만 동작하며, 배포(prod)에서는 기본으로 꺼져 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    static final String DEMO_OWNER_USERNAME = "demo_owner";
    static final String DEMO_ADMIN_USERNAME = "demo_admin";
    static final String DEMO_PASSWORD = "demo1234!";
    /** 실제로 쓰이지 않는 번호 (개발 중 문자는 로그로만 남는다) */
    static final String DEMO_OWNER_PHONE = "01000000000";

    /** 데모 운영자가 맡을 지점 코드와 수용량 (강남역점은 자리 마감을 시험해보기 좋게 작게) */
    private static final Map<String, Integer> DEMO_BRANCHES = Map.of(
        "HONGDAE", 20,
        "SINCHON", 10,
        "GANGNAM", 3
    );

    private final MemberRepository memberRepository;
    private final StoragePlaceRepository storagePlaceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Member owner = memberRepository.findByUsername(DEMO_OWNER_USERNAME)
            .orElseGet(() -> memberRepository.save(Member.createOwner(
                DEMO_OWNER_USERNAME, passwordEncoder.encode(DEMO_PASSWORD), "데모 사장님", null, DEMO_OWNER_PHONE)));
        if (owner.getPhoneNumber() == null && !memberRepository.existsByPhoneNumber(DEMO_OWNER_PHONE)) {
            owner.changePhone(DEMO_OWNER_PHONE);
        }

        if (!memberRepository.existsByUsername(DEMO_ADMIN_USERNAME)) {
            memberRepository.save(Member.createAdmin(DEMO_ADMIN_USERNAME, passwordEncoder.encode(DEMO_PASSWORD), "데모 관리자"));
            log.info("=== 데모 관리자 {} 를 만들었습니다 ===", DEMO_ADMIN_USERNAME);
        }

        DEMO_BRANCHES.forEach((code, capacity) -> storagePlaceRepository.findByCode(code)
            .filter(place -> !place.isOperating())
            .ifPresent(place -> {
                place.claim(owner, capacity, null);
                log.info("=== 데모 운영자 {}가 {}을(를) 맡았습니다 (수용량 {}) ===", DEMO_OWNER_USERNAME, place.getName(), capacity);
            }));
    }
}
