package com.luggagestorage.notification;

import java.util.Map;

/** 실제 문자·알림톡을 내보내는 곳. 개발은 로그로만 남기고, 배포는 솔라피로 보낸다 */
public interface NotificationSender {

    /** @return 실제로 나간 경로 (SMS, ALIMTALK, LOG) */
    String send(String phoneNumber, NotificationType type, String text, Map<String, String> variables);
}
