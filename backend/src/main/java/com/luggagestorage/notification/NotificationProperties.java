package com.luggagestorage.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * @param provider log(개발: 서버 로그에만 남김) 또는 solapi(배포: 실제 문자·알림톡 발송)
 * @param exposeCode 인증번호를 응답에 담아 줄지. 문자가 실제로 가지 않는 개발 환경에서만 true
 */
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(String provider, boolean exposeCode, Solapi solapi) {

    public boolean isSolapi() {
        return "solapi".equalsIgnoreCase(provider);
    }

    /**
     * @param sender 문자 발신번호 (솔라피에 등록·인증한 번호)
     * @param pfId 카카오 알림톡 채널(비즈니스 채널) ID. 비우면 문자로만 보낸다
     * @param templates 알림 종류별 승인된 알림톡 템플릿 ID. 없는 종류는 문자로 보낸다
     */
    public record Solapi(String apiKey, String apiSecret, String sender, String pfId, Map<String, String> templates) {

        public String templateIdOf(NotificationType type) {
            if (pfId == null || pfId.isBlank() || templates == null) {
                return null;
            }
            String templateId = templates.get(type.name());
            return templateId == null || templateId.isBlank() ? null : templateId;
        }
    }
}
