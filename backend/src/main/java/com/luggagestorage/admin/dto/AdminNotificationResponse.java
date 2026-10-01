package com.luggagestorage.admin.dto;

import com.luggagestorage.notification.Notification;
import com.luggagestorage.notification.NotificationType;
import com.luggagestorage.verification.PhoneNumbers;

import java.time.LocalDateTime;

public record AdminNotificationResponse(
    Long id,
    LocalDateTime createdAt,
    String phoneNumber,
    NotificationType type,
    String channel,
    String content,
    Notification.Status status,
    String error
) {

    /** 인증번호는 발송 기록에 애초에 가려진 채로 저장된다 (NotificationType.renderForLog) */
    public static AdminNotificationResponse from(Notification notification) {
        String content = notification.getContent();
        return new AdminNotificationResponse(
            notification.getId(),
            notification.getCreatedAt(),
            PhoneNumbers.format(notification.getPhoneNumber()),
            notification.getType(),
            notification.getChannel(),
            content,
            notification.getStatus(),
            notification.getError()
        );
    }
}
