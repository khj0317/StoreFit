package com.luggagestorage.store.entity;

import com.luggagestorage.common.entity.BaseTimeEntity;
import com.luggagestorage.member.entity.Member;
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

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String address;

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

    public Store(Member member, String name, String description, String address, StoreCategory category,
                 Integer luggageCount, LocalDate startDate, LocalDate endDate) {
        this.member = member;
        this.name = name;
        this.description = description;
        this.address = address;
        this.category = category;
        this.luggageCount = luggageCount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = StoreStatus.PENDING;
    }

    public void update(String name, String description, String address, StoreCategory category,
                        Integer luggageCount, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.category = category;
        this.luggageCount = luggageCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void pickUp() {
        this.status = StoreStatus.PICKED_UP;
    }

    public void beginStorage() {
        this.status = StoreStatus.IN_USE;
    }

    public void complete() {
        this.status = StoreStatus.COMPLETED;
    }

    public void cancel() {
        this.status = StoreStatus.CANCELLED;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.member.getId().equals(memberId);
    }
}
