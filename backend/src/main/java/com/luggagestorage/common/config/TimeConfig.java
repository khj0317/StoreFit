package com.luggagestorage.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "오늘"은 항상 한국 시간 기준이다. 배포 서버(Render)는 UTC라서 LocalDate.now()를 그대로 쓰면
 * 오전 9시 전까지 어제 날짜가 된다. 서비스는 이 Clock으로 날짜를 구하고, 테스트에서는 고정된
 * Clock을 넣어 날짜에 따라 달라지는 규칙(환불, 체크인 가능일)을 검증한다.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SERVICE_ZONE);
    }
}
