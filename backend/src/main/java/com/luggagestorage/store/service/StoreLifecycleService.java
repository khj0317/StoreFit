package com.luggagestorage.store.service;

import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.repository.PaymentRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 시간이 지나면 저절로 바뀌어야 하는 예약 상태를 처리한다. StoreScheduler가 주기적으로 부르고,
 * 테스트에서는 직접 불러서 검증한다.
 * - 결제 마감이 지난 미결제 예약 → EXPIRED (자리 반환)
 * - 결제했지만 시작일에 체크인하지 않은 예약 → NO_SHOW (자리 반환, 환불 없음)
 * - 찾는 날 하루 전 알림, 찾는 날이 지난 짐 연체 알림 (하루 한 번)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreLifecycleService {

    private final StoreRepository storeRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationNotifier reservationNotifier;
    private final Clock clock;
    private final JdbcTemplate jdbcTemplate;

    private static final String DAILY_NOTICES_JOB = "DAILY_NOTICES";

    /** @return 자동 취소한 예약 수 */
    @Transactional
    public int expireUnpaidReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        int expired = 0;
        for (Long storeId : storeRepository.findExpiredUnpaidIds(now)) {
            // 결제 승인과 동시에 일어날 수 있으니 잠그고 다시 확인한다 (PaymentService.confirm도 같은 행을 잠근다)
            Store store = storeRepository.findByIdForUpdate(storeId).orElse(null);
            if (store == null || store.getStatus() != StoreStatus.PENDING || !store.isPaymentDeadlinePassed(now)
                || store.getCheckInCode() != null) {
                continue;
            }
            store.expire();
            paymentRepository.findByStore(store).ifPresent(Payment::expire);
            reservationNotifier.paymentExpired(store);
            expired++;
        }
        if (expired > 0) {
            log.info("결제 마감이 지난 예약 {}건을 자동 취소했습니다.", expired);
        }
        return expired;
    }

    /** @return 노쇼 처리한 예약 수 */
    @Transactional
    public int markNoShows() {
        List<Store> stores = storeRepository.findPaidNotCheckedInBefore(LocalDate.now(clock));
        stores.forEach(store -> {
            store.markNoShow();
            reservationNotifier.noShow(store);
        });
        if (!stores.isEmpty()) {
            log.info("시작일에 체크인하지 않은 예약 {}건을 노쇼 처리했습니다.", stores.size());
        }
        return stores.size();
    }

    /**
     * 오늘 찾는 날 알림·연체 알림을 아직 보내지 않았으면 보낸다. 서버가 잠들었다 깨어나도 하루에 한 번만 나가도록
     * daily_job_runs에 오늘 날짜를 먼저 넣고, 이미 있으면(다른 실행이 보냈으면) 건너뛴다.
     * 알림 기록이 저장되다 실패하면 같은 트랜잭션이라 날짜 기록도 함께 취소돼서 다음에 다시 시도한다.
     *
     * @return 이번에 보냈으면 true
     */
    @Transactional
    public boolean sendDailyNoticesOnce() {
        int claimed = jdbcTemplate.update(
            "insert into daily_job_runs (job_name, run_date) values (?, ?) on conflict do nothing",
            DAILY_NOTICES_JOB, LocalDate.now(clock));
        if (claimed == 0) {
            return false;
        }
        int reminders = sendPickupReminders();
        int overdue = sendOverdueNotices();
        log.info("오늘의 알림을 보냈습니다. 찾는 날 알림 {}건, 연체 알림 {}건", reminders, overdue);
        return true;
    }

    /** 내일이 찾는 날인 보관 중 짐에 알림을 보낸다 */
    @Transactional
    public int sendPickupReminders() {
        List<Store> stores = storeRepository.findByStatusAndEndDate(StoreStatus.IN_USE, LocalDate.now(clock).plusDays(1));
        stores.forEach(reservationNotifier::pickupReminder);
        return stores.size();
    }

    /** 찾는 날이 지난 보관 중 짐에 매일 연체 알림을 보낸다 */
    @Transactional
    public int sendOverdueNotices() {
        LocalDate today = LocalDate.now(clock);
        List<Store> stores = storeRepository.findByStatusAndEndDateBefore(StoreStatus.IN_USE, today);
        stores.forEach(store -> reservationNotifier.overdue(store, OverduePolicy.overdueDays(store, today)));
        return stores.size();
    }
}
