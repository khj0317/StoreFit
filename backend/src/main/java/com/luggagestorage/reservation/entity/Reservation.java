package com.luggagestorage.reservation.entity;

import com.luggagestorage.common.entity.BaseTimeEntity;
import com.luggagestorage.member.entity.Member;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private Integer luggageCount;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    public Reservation(Member member, Store store, Integer luggageCount,
                        LocalDateTime startTime, LocalDateTime endTime) {
        this.member = member;
        this.store = store;
        this.luggageCount = luggageCount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = ReservationStatus.PENDING;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void start() {
        this.status = ReservationStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }

    public boolean isHostedBy(Long memberId) {
        return this.store.isOwnedBy(memberId);
    }
}
