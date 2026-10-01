package com.luggagestorage.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {

    public static final String EXECUTOR = "notificationExecutor";

    @Bean
    public NotificationSender notificationSender(NotificationProperties properties) {
        if (properties.isSolapi()) {
            log.info("알림 발송: 솔라피 (문자·카카오 알림톡)");
            return new SolapiNotificationSender(properties.solapi());
        }
        log.info("알림 발송: 개발용 로그 (실제 문자는 보내지 않음)");
        return new LogNotificationSender();
    }

    /** 문자 발송은 외부 API라 느릴 수 있어서, 요청을 붙잡지 않도록 작은 스레드 풀에서 보낸다 */
    @Bean(name = EXECUTOR)
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notify-");
        executor.initialize();
        return executor;
    }
}
