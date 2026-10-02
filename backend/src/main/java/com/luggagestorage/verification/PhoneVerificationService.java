package com.luggagestorage.verification;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.notification.NotificationProperties;
import com.luggagestorage.notification.NotificationService;
import com.luggagestorage.notification.NotificationType;
import com.luggagestorage.verification.dto.SendCodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * 휴대폰 인증: 인증번호 발송 → 확인 → 일회용 토큰 발급 → 가입·아이디 찾기 등에서 토큰 사용.
 * 문자 비용과 무작위 대입을 막기 위해 재전송 간격, 시간당 발송 횟수, 입력 시도 횟수를 제한한다.
 * 번호를 바꿔 가며 요청하는 남용은 IP별 한도와 하루 전체 상한(VerificationLimits)으로 막는다.
 */
@Service
@EnableConfigurationProperties(VerificationLimits.class)
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneVerificationService {

    static final Duration CODE_TTL = Duration.ofMinutes(3);
    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_SENDS_PER_HOUR = 5;

    private final PhoneVerificationRepository verificationRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;
    private final PasswordEncoder passwordEncoder;
    private final VerificationLimits limits;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public SendCodeResponse sendCode(String rawPhone, VerificationPurpose purpose, String requestIp) {
        String phone = PhoneNumbers.normalize(rawPhone);
        LocalDateTime now = LocalDateTime.now(clock);

        boolean registered = memberRepository.existsByPhoneNumber(phone);
        if ((purpose == VerificationPurpose.SIGNUP || purpose == VerificationPurpose.CHANGE_PHONE) && registered) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE);
        }
        if ((purpose == VerificationPurpose.FIND_USERNAME || purpose == VerificationPurpose.RESET_PASSWORD) && !registered) {
            throw new BusinessException(ErrorCode.PHONE_NOT_REGISTERED);
        }

        verificationRepository.findFirstByPhoneNumberAndPurposeOrderByIdDesc(phone, purpose)
            .filter(last -> last.getCreatedAt().plus(RESEND_COOLDOWN).isAfter(now))
            .ifPresent(last -> {
                throw new BusinessException(ErrorCode.VERIFICATION_TOO_SOON);
            });
        if (verificationRepository.countByPhoneNumberAndCreatedAtAfter(phone, now.minusHours(1)) >= MAX_SENDS_PER_HOUR) {
            throw new BusinessException(ErrorCode.VERIFICATION_TOO_MANY);
        }
        if (requestIp != null
            && verificationRepository.countByRequestIpAndCreatedAtAfter(requestIp, now.minusHours(1)) >= limits.maxSendsPerIpPerHour()) {
            throw new BusinessException(ErrorCode.VERIFICATION_IP_LIMIT);
        }
        if (limits.dailyLimit() > 0
            && verificationRepository.countByCreatedAtGreaterThanEqual(LocalDate.now(clock).atStartOfDay()) >= limits.dailyLimit()) {
            throw new BusinessException(ErrorCode.VERIFICATION_DAILY_LIMIT);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        verificationRepository.save(new PhoneVerification(phone, purpose, passwordEncoder.encode(code), now, now.plus(CODE_TTL), requestIp));
        notificationService.send(null, phone, NotificationType.VERIFICATION_CODE, Map.of("code", code));

        return new SendCodeResponse(CODE_TTL.toSeconds(), notificationProperties.exposeCode() ? code : null);
    }

    /** 인증번호가 맞으면 30분 동안 한 번 쓸 수 있는 토큰을 준다 */
    @Transactional(noRollbackFor = BusinessException.class)
    public String verifyCode(String rawPhone, VerificationPurpose purpose, String code) {
        String phone = PhoneNumbers.normalize(rawPhone);
        LocalDateTime now = LocalDateTime.now(clock);

        PhoneVerification verification = verificationRepository.findFirstByPhoneNumberAndPurposeOrderByIdDesc(phone, purpose)
            .filter(found -> found.canAttempt(now))
            .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_EXPIRED));

        if (code == null || !passwordEncoder.matches(code.trim(), verification.getCodeHash())) {
            // 틀린 횟수는 롤백되지 않고 남아야 5번 넘게 시도하는 걸 막을 수 있다
            verification.recordFailedAttempt();
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        verification.verify(token, now, now.plus(TOKEN_TTL));
        return token;
    }

    /**
     * 가입·아이디 찾기 같은 요청에서 토큰을 쓴다. 목적과 번호가 맞아야 하고, 한 번 쓰면 다시 쓸 수 없다.
     * @return 인증된 휴대폰 번호 (숫자만)
     */
    @Transactional
    public String consume(String token, VerificationPurpose purpose, String rawPhone) {
        String phone = PhoneNumbers.normalize(rawPhone);
        LocalDateTime now = LocalDateTime.now(clock);
        PhoneVerification verification = (token == null ? null : verificationRepository.findByTokenForUpdate(token).orElse(null));
        if (verification == null
            || verification.getPurpose() != purpose
            || !verification.getPhoneNumber().equals(phone)
            || !verification.isTokenUsable(now)) {
            throw new BusinessException(ErrorCode.PHONE_NOT_VERIFIED);
        }
        verification.markUsed(now);
        return phone;
    }
}
