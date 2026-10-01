package com.luggagestorage.store.entity;

import com.luggagestorage.common.entity.BaseTimeEntity;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.place.entity.StoragePlace;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 이용자의 짐 보관 예약 한 건 */
@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private StoragePlace place;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreCategory category;

    @Column(nullable = false)
    private Integer luggageCount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status;

    @Column(nullable = false)
    private Integer totalPrice;

    /** 결제가 끝나면 발급되는 QR 내용. 운영자가 이 코드를 스캔해서 체크인·체크아웃한다 */
    @Column(unique = true, length = 16)
    private String checkInCode;

    private LocalDateTime checkedInAt;

    private LocalDateTime checkedOutAt;

    /** 이 시각까지 결제하지 않으면 자동 취소된다. 결제를 마치면 null */
    private LocalDateTime paymentDeadline;

    public Store(Member member, StoragePlace place, String name, String description, StoreCategory category,
                 Integer luggageCount, LocalDate startDate, LocalDate endDate, Integer totalPrice) {
        this.member = member;
        this.place = place;
        this.name = name;
        this.description = description;
        this.category = category;
        this.luggageCount = luggageCount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = StoreStatus.PENDING;
        this.totalPrice = totalPrice;
    }

    public void update(StoragePlace place, String name, String description, StoreCategory category,
                       Integer luggageCount, LocalDate startDate, LocalDate endDate, Integer totalPrice) {
        this.place = place;
        this.name = name;
        this.description = description;
        this.category = category;
        this.luggageCount = luggageCount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalPrice = totalPrice;
    }

    public void issueCheckInCode(String code) {
        this.checkInCode = code;
        this.paymentDeadline = null;
    }

    public void setPaymentDeadline(LocalDateTime paymentDeadline) {
        this.paymentDeadline = paymentDeadline;
    }

    public boolean isPaymentDeadlinePassed(LocalDateTime now) {
        return paymentDeadline != null && now.isAfter(paymentDeadline);
    }

    public void expire() {
        this.status = StoreStatus.EXPIRED;
    }

    public void markNoShow() {
        this.status = StoreStatus.NO_SHOW;
    }

    public void checkIn(LocalDateTime now) {
        this.status = StoreStatus.IN_USE;
        this.checkedInAt = now;
    }

    public void checkOut(LocalDateTime now) {
        this.status = StoreStatus.COMPLETED;
        this.checkedOutAt = now;
    }

    public void cancel() {
        this.status = StoreStatus.CANCELED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }
}
