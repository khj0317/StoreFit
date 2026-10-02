package com.luggagestorage.store.service;

import com.luggagestorage.demo.DemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;

/**
 * 예약 상태 자동 처리 일정 (모두 한국 시간 기준).
 * 배포 서버(Render 무료 플랜)는 쉬는 동안 잠들어서 "매일 0시 5분" 같은 정해진 시각에 깨어 있다는 보장이 없다.
 * 그래서 정해진 시각에 한 번 돌리는 대신, 깨어 있는 동안 자주 확인하면서 밀린 일을 처리한다.
 * - 노쇼 처리: "시작일이 지났는데 체크인 안 한 예약"을 찾는 방식이라 언제 돌려도 결과가 같다.
 * - 찾는 날·연체 알림: 오전 10시~밤 9시 사이에 깨어 있을 때 하루 한 번만 보낸다 (StoreLifecycleService.sendDailyNoticesOnce).
 * - 체험 데이터: 그날 처음 깨어 있을 때 처음 상태로 되돌린다 (DemoService.resetOncePerDay).
 * 서버가 한 대라 중복 실행 걱정 없이 단순한 @Scheduled를 쓴다.
 * 테스트에서는 app.scheduler.enabled=false로 끄고 StoreLifecycleService를 직접 부른다.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class StoreScheduler {

    /** 이 시간대에만 알림 문자를 보낸다 (밤늦게·새벽에 깨어나도 보내지 않는다) */
    private static final LocalTime NOTICE_FROM = LocalTime.of(10, 0);
    private static final LocalTime NOTICE_UNTIL = LocalTime.of(21, 0);

    private final StoreLifecycleService lifecycleService;
    private final DemoService demoService;
    private final Clock clock;

    /** 1분마다 결제 마감이 지난 예약을 취소한다 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void expireUnpaid() {
        lifecycleService.expireUnpaidReservations();
    }

    /** 서버가 켜지고 잠시 뒤, 그리고 10분마다: 체험 데이터 초기화, 밀린 노쇼 처리, 오늘의 알림 */
    @Scheduled(fixedDelay = 600_000, initialDelay = 45_000)
    public void catchUpDailyWork() {
        demoService.resetOncePerDay();
        lifecycleService.markNoShows();
        LocalTime now = LocalTime.now(clock);
        if (!now.isBefore(NOTICE_FROM) && now.isBefore(NOTICE_UNTIL)) {
            lifecycleService.sendDailyNoticesOnce();
        }
    }
}
