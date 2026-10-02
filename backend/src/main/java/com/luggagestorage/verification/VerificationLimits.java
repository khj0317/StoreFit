package com.luggagestorage.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증 문자 남용 방지 한도. 번호당 제한(재전송 60초·시간당 5회)만으로는 번호를 바꿔 가며 요청하는 걸 막을 수 없어서
 * 요청한 IP별 한도와, 문자 비용·발송 한도를 지키기 위한 하루 전체 상한을 둔다.
 *
 * @param maxSendsPerIpPerHour 한 IP에서 1시간 동안 보낼 수 있는 인증 문자 수
 * @param dailyLimit 하루(한국 시간) 전체 인증 문자 수. 0 이하면 제한하지 않는다
 */
@ConfigurationProperties(prefix = "app.verification")
public record VerificationLimits(int maxSendsPerIpPerHour, int dailyLimit) {
}
