package com.luggagestorage.payment.entity;

import com.luggagestorage.common.entity.BaseTimeEntity;
import com.luggagestorage.store.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String paymentKey;

    private String method;

    private LocalDateTime approvedAt;

    public Payment(Store store, String orderId, Integer amount) {
        this.store = store;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    public void approve(String paymentKey, String method, LocalDateTime approvedAt) {
        this.status = PaymentStatus.DONE;
        this.paymentKey = paymentKey;
        this.method = method;
        this.approvedAt = approvedAt;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.store.isOwnedBy(memberId);
    }
}
