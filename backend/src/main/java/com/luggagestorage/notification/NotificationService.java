package com.luggagestorage.notification;

import com.luggagestorage.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 알림 요청은 호출한 트랜잭션 안에서 기록만 남기고, 실제 발송은 그 트랜잭션이 커밋된 뒤에 한다.
 * 그래서 예약 저장이 실패(롤백)하면 "예약 확정" 문자도 나가지 않고, 문자 API가 느려도 요청이 기다리지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** text는 실제로 보낼 원문. DB에는 민감한 값(인증번호)을 가린 문구만 남기므로 원문은 이벤트로 넘긴다 */
    public record NotificationRequested(Long notificationId, String text, Map<String, String> variables) {
    }

    /** 회원에게 보낸다. 번호가 없는 회원(예전 계정·관리자)은 건너뛴다 */
    @Transactional(propagation = Propagation.REQUIRED)
    public void notify(Member member, NotificationType type, Map<String, String> variables) {
        if (member == null || member.getPhoneNumber() == null || member.getPhoneNumber().isBlank()) {
            return;
        }
        send(member, member.getPhoneNumber(), type, variables);
    }

    /** 아직 회원이 아닌 번호(가입 인증)에도 보낼 수 있다 */
    @Transactional(propagation = Propagation.REQUIRED)
    public void send(Member member, String phoneNumber, NotificationType type, Map<String, String> variables) {
        String text = type.render(variables);
        Notification notification = notificationRepository.save(
            new Notification(member, phoneNumber, type, type.renderForLog(variables), LocalDateTime.now(clock)));
        eventPublisher.publishEvent(new NotificationRequested(notification.getId(), text, variables));
    }

    @Async(NotificationConfig.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationRequested event) {
        Notification notification = notificationRepository.findById(event.notificationId()).orElse(null);
        if (notification == null) {
            return;
        }
        try {
            String channel = notificationSender.send(
                notification.getPhoneNumber(), notification.getType(), event.text(), event.variables());
            notification.markSent(channel, LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            log.warn("알림 발송 실패 {} → {}: {}", notification.getType(), notification.getPhoneNumber(), e.getMessage());
            notification.markFailed(e.getMessage());
        }
    }
}
