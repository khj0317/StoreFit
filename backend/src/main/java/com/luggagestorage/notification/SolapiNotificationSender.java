package com.luggagestorage.notification;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 솔라피(SOLAPI)로 문자·카카오 알림톡을 보낸다.
 * 알림 종류에 승인된 알림톡 템플릿이 있으면 알림톡으로 보내고, 알림톡이 실패하면 솔라피가 같은 내용을
 * 문자로 대신 보낸다(disableSms=false). 템플릿이 없으면 처음부터 문자로 보낸다.
 * API 문서: https://developers.solapi.com
 */
public class SolapiNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final NotificationProperties.Solapi properties;

    public SolapiNotificationSender(NotificationProperties.Solapi properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl("https://api.solapi.com").build();
    }

    @Override
    public String send(String phoneNumber, NotificationType type, String text, Map<String, String> variables) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("to", phoneNumber);
        message.put("from", properties.sender());
        message.put("text", text);

        String templateId = properties.templateIdOf(type);
        if (templateId != null) {
            Map<String, String> kakaoVariables = new HashMap<>();
            variables.forEach((key, value) -> kakaoVariables.put("#{" + key + "}", value));
            message.put("kakaoOptions", Map.of(
                "pfId", properties.pfId(),
                "templateId", templateId,
                "variables", kakaoVariables,
                "disableSms", false
            ));
        }

        restClient.post()
            .uri("/messages/v4/send")
            .header("Authorization", authorization())
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("message", message))
            .retrieve()
            .toBodilessEntity();
        return templateId != null ? "ALIMTALK" : "SMS";
    }

    /** HMAC-SHA256 apiKey=..., date=..., salt=..., signature=HMAC(secret, date + salt) */
    private String authorization() {
        String date = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.apiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = HexFormat.of().formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
            return "HMAC-SHA256 apiKey=" + properties.apiKey() + ", date=" + date + ", salt=" + salt + ", signature=" + signature;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("솔라피 서명을 만들지 못했습니다.", e);
        }
    }
}
