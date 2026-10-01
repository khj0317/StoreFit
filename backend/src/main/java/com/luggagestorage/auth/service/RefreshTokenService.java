package com.luggagestorage.auth.service;

import com.luggagestorage.auth.entity.RefreshToken;
import com.luggagestorage.auth.repository.RefreshTokenRepository;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 액세스 토큰은 짧게(30분), 리프레시 토큰은 길게(14일) 둔다. 리프레시 토큰은 쓸 때마다 새것으로 바꾸고,
 * 이미 바꿔서 버린 토큰이 다시 들어오면 탈취로 보고 그 회원의 로그인을 모두 끊는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Value("${jwt.refresh-token-expiration-days:14}")
    private long refreshTokenDays;

    public record Rotated(Member member, String refreshToken) {
    }

    @Transactional
    public String issue(Member member) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now(clock);
        refreshTokenRepository.save(new RefreshToken(member, hash(raw), now, now.plus(Duration.ofDays(refreshTokenDays))));
        return raw;
    }

    // 재사용 감지 시 모든 토큰을 끊은 결과는 예외와 함께 롤백되지 않고 남아야 한다
    @Transactional(noRollbackFor = BusinessException.class)
    public Rotated rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        RefreshToken token = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (token.isRevoked()) {
            revokeAll(token.getMember());
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!token.isUsable(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        token.revoke(now);
        return new Rotated(token.getMember(), issue(token.getMember()));
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
            .ifPresent(token -> token.revoke(LocalDateTime.now(clock)));
    }

    @Transactional
    public void revokeAll(Member member) {
        LocalDateTime now = LocalDateTime.now(clock);
        refreshTokenRepository.findByMemberAndRevokedAtIsNull(member).forEach(token -> token.revoke(now));
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
