package com.luggagestorage.store.repository;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByMemberOrderByCreatedAtDesc(Member member);

    boolean existsByCheckInCode(String checkInCode);

    /** 결제·자동 취소처럼 한 예약을 동시에 바꿀 수 있는 곳은 행을 잠그고 상태를 다시 확인한다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s where s.id = :id")
    Optional<Store> findByIdForUpdate(@Param("id") Long id);

    /** 결제 마감이 지났는데 아직 결제하지 않은 예약 */
    @Query("""
        select s.id from Store s
        where s.status = com.luggagestorage.store.entity.StoreStatus.PENDING
          and s.paymentDeadline < :now
          and not exists (
            select p from Payment p where p.store = s and p.status = com.luggagestorage.payment.entity.PaymentStatus.DONE)
        """)
    List<Long> findExpiredUnpaidIds(@Param("now") LocalDateTime now);

    /** 결제했지만 보관 시작일이 지나도록 체크인하지 않은 예약 */
    @Query("""
        select s from Store s join fetch s.member join fetch s.place
        where s.status = com.luggagestorage.store.entity.StoreStatus.PENDING
          and s.startDate < :today
          and exists (
            select p from Payment p where p.store = s and p.status = com.luggagestorage.payment.entity.PaymentStatus.DONE)
        """)
    List<Store> findPaidNotCheckedInBefore(@Param("today") LocalDate today);

    @Query("select s from Store s join fetch s.member join fetch s.place where s.status = :status and s.endDate = :endDate")
    List<Store> findByStatusAndEndDate(@Param("status") StoreStatus status, @Param("endDate") LocalDate endDate);

    @Query("select s from Store s join fetch s.member join fetch s.place where s.status = :status and s.endDate < :date")
    List<Store> findByStatusAndEndDateBefore(@Param("status") StoreStatus status, @Param("date") LocalDate date);

    /** 체크인·체크아웃은 같은 예약을 두 번 처리하지 않도록 행을 잠그고 상태를 확인한다 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s join fetch s.place join fetch s.member where s.checkInCode = :code")
    Optional<Store> findByCheckInCodeForUpdate(@Param("code") String code);

    @Query("select s from Store s join fetch s.place join fetch s.member where s.checkInCode = :code")
    Optional<Store> findByCheckInCode(@Param("code") String code);

    /** [startDate, endDate]와 하루라도 겹치는, 자리를 차지하는 예약 */
    @Query("""
        select s from Store s
        where s.place in :places
          and s.status in :statuses
          and s.startDate <= :endDate
          and s.endDate >= :startDate
        """)
    List<Store> findOverlapping(
        @Param("places") Collection<StoragePlace> places,
        @Param("statuses") Collection<StoreStatus> statuses,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /** 운영자 화면: 내 보관소들의 진행 중 예약 (오래된 날짜부터) */
    @Query("""
        select s from Store s join fetch s.place join fetch s.member
        where s.place in :places and s.status in :statuses
        order by s.startDate asc, s.id asc
        """)
    List<Store> findByPlacesAndStatuses(
        @Param("places") Collection<StoragePlace> places,
        @Param("statuses") Collection<StoreStatus> statuses
    );
}
