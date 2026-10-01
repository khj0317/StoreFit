package com.luggagestorage.auth.service;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비밀번호 무작위 대입 방지: 같은 아이디로 5번 연속 틀리면 10분 동안 로그인을 막는다.
 * 서버가 한 대(Render 무료 플랜)라 메모리에 둔다. 서버를 여러 대로 늘리면 Redis 같은 공용 저장소로 옮겨야 한다.
 * 없는 아이디도 똑같이 세서, 잠김 여부로 아이디가 있는지 알아낼 수 없게 한다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private final Clock clock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    private record Attempts(int failures, Instant lockedUntil) {
    }

    public void checkNotLocked(String username) {
        Attempts current = attempts.get(key(username));
        if (current != null && current.lockedUntil() != null && Instant.now(clock).isBefore(current.lockedUntil())) {
            throw new BusinessException(ErrorCode.TOO_MANY_LOGIN_ATTEMPTS);
        }
    }

    public void recordFailure(String username) {
        Instant now = Instant.now(clock);
        attempts.compute(key(username), (key, current) -> {
            int failures = (current == null || isExpiredLock(current, now)) ? 1 : current.failures() + 1;
            return new Attempts(failures, failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : null);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    private static boolean isExpiredLock(Attempts attempts, Instant now) {
        return attempts.lockedUntil() != null && !now.isBefore(attempts.lockedUntil());
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
