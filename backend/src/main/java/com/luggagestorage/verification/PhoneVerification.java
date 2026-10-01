package com.luggagestorage.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 휴대폰 인증 한 건. 인증번호는 해시로만 저장하고, 맞히면 한 번만 쓸 수 있는 토큰을 발급한다.
 * 가입·아이디 찾기 같은 실제 요청은 번호 대신 이 토큰을 받아서, 인증과 요청 사이에 번호를 바꿔치기할 수 없다.
 */
@Entity
@Table(name = "phone_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {

    static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationPurpose purpose;

    @Column(nullable = false, length = 100)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Integer attempts;

    private LocalDateTime verifiedAt;

    @Column(unique = true, length = 64)
    private String token;

    private LocalDateTime tokenExpiresAt;

    private LocalDateTime usedAt;

    public PhoneVerification(String phoneNumber, VerificationPurpose purpose, String codeHash,
                             LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.phoneNumber = phoneNumber;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attempts = 0;
    }

    public boolean canAttempt(LocalDateTime now) {
        return verifiedAt == null && attempts < MAX_ATTEMPTS && now.isBefore(expiresAt);
    }

    public void recordFailedAttempt() {
        this.attempts++;
    }

    public void verify(String token, LocalDateTime now, LocalDateTime tokenExpiresAt) {
        this.verifiedAt = now;
        this.token = token;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public boolean isTokenUsable(LocalDateTime now) {
        return token != null && usedAt == null && now.isBefore(tokenExpiresAt);
    }

    public void markUsed(LocalDateTime now) {
        this.usedAt = now;
    }
}
