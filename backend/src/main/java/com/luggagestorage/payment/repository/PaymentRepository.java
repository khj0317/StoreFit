package com.luggagestorage.payment.repository;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.place.entity.StoragePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStore(Store store);

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByStore_MemberOrderByCreatedAtDesc(Member member);

    List<Payment> findByStoreIn(Collection<Store> stores);

    /** 운영자 매출: 기간 안에 승인된 결제에서 환불한 금액을 뺀 합계 */
    @Query("""
        select coalesce(sum(p.amount - p.canceledAmount), 0) from Payment p
        where p.store.place in :places
          and p.approvedAt >= :from and p.approvedAt < :to
        """)
    long sumNetRevenue(
        @Param("places") Collection<StoragePlace> places,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
}
