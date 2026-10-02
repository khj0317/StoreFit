package com.luggagestorage.verification;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.notification.NotificationProperties;
import com.luggagestorage.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 하루 전체 발송 상한: 번호·IP가 모두 달라도 그날 보낸 인증 문자가 상한에 닿으면 더 보내지 않는다 */
class PhoneVerificationServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-02T05:00:00Z"), SEOUL); // 한국 시간 오후 2시

    private final PhoneVerificationRepository repository = mock(PhoneVerificationRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private PhoneVerificationService serviceWithDailyLimit(int dailyLimit) {
        return new PhoneVerificationService(repository, memberRepository, notificationService,
            new NotificationProperties("log", false, null), mock(PasswordEncoder.class),
            new VerificationLimits(10, dailyLimit), clock);
    }

    @Test
    void dailyLimitReached_noMoreCodesAreSentToday() {
        when(repository.findFirstByPhoneNumberAndPurposeOrderByIdDesc(anyString(), any())).thenReturn(Optional.empty());
        // 오늘 0시(한국 시간)부터 센다
        when(repository.countByCreatedAtGreaterThanEqual(LocalDateTime.of(2026, 10, 2, 0, 0))).thenReturn(30L);

        assertThatThrownBy(() -> serviceWithDailyLimit(30).sendCode("01012345678", VerificationPurpose.SIGNUP, "198.51.100.7"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.VERIFICATION_DAILY_LIMIT);
        verify(notificationService, never()).send(any(), anyString(), any(), any());
    }
}
