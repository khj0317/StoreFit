package com.luggagestorage.notification;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/** 개발용: 문자를 보내지 않고 서버 로그에 남긴다 (발송 기록은 notifications 테이블에도 남는다) */
@Slf4j
public class LogNotificationSender implements NotificationSender {

    @Override
    public String send(String phoneNumber, NotificationType type, String text, Map<String, String> variables) {
        log.info("[알림 · 개발용] {} → {}\n{}", type, phoneNumber, text);
        return "LOG";
    }
}
