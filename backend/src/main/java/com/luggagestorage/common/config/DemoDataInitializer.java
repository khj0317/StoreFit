package com.luggagestorage.common.config;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬에서 본사 관리 화면(지점 등록·운영 신청 심사)을 바로 써볼 수 있도록 데모 관리자를 만든다.
 * 체험 이용자·사장님은 DemoService가 따로 만든다 (로그인 화면의 "둘러보기").
 * 비밀번호가 소스에 있으므로 app.demo-data.enabled=true 인 로컬에서만 동작하고, 배포(prod)에서는 꺼져 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {

    static final String DEMO_ADMIN_USERNAME = "demo_admin";
    static final String DEMO_PASSWORD = "demo1234!";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!memberRepository.existsByUsername(DEMO_ADMIN_USERNAME)) {
            memberRepository.save(Member.createAdmin(DEMO_ADMIN_USERNAME, passwordEncoder.encode(DEMO_PASSWORD), "데모 관리자"));
            log.info("=== 데모 관리자 {} 를 만들었습니다 ===", DEMO_ADMIN_USERNAME);
        }
    }
}
