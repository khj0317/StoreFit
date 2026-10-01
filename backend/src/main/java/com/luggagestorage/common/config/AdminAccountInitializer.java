package com.luggagestorage.common.config;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 계정은 회원가입으로 만들 수 없다. 배포 환경 변수 ADMIN_USERNAME·ADMIN_PASSWORD를 둘 다 넣으면
 * 서버가 켜질 때 그 계정이 없을 경우 만든다 (이미 있으면 비밀번호를 덮어쓰지 않는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String username;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (memberRepository.existsByUsername(username)) {
            return;
        }
        memberRepository.save(Member.createAdmin(username, passwordEncoder.encode(password), "스토어핏 관리자"));
        log.info("관리자 계정 {}을(를) 만들었습니다.", username);
    }
}
