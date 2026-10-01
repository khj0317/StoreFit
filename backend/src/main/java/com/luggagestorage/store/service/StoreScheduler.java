package com.luggagestorage.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 예약 상태 자동 처리 일정 (모두 한국 시간 기준).
 * 서버가 한 대(Render 무료 플랜)라 중복 실행 걱정 없이 단순한 @Scheduled를 쓴다.
 * 테스트에서는 app.scheduler.enabled=false로 끄고 StoreLifecycleService를 직접 부른다.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class StoreScheduler {

    private final StoreLifecycleService lifecycleService;

    /** 1분마다 결제 마감이 지난 예약을 취소한다 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void expireUnpaid() {
        lifecycleService.expireUnpaidReservations();
    }

    /** 매일 0시 5분: 어제가 시작일이었는데 체크인하지 않은 예약을 노쇼 처리한다 */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void markNoShows() {
        lifecycleService.markNoShows();
    }

    /** 매일 오전 10시: 내일 찾을 짐 알림, 기간이 지난 짐 연체 알림 */
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void sendDailyNotices() {
        lifecycleService.sendPickupReminders();
        lifecycleService.sendOverdueNotices();
    }
}
