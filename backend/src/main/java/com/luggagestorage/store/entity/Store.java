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

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private Integer luggageCount;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreStatus status;

    public Store(Member member, String name, String description, String address,
                 Integer luggageCount, LocalDateTime startTime, LocalDateTime endTime) {
        this.member = member;
        this.name = name;
        this.description = description;
        this.address = address;
        this.luggageCount = luggageCount;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = StoreStatus.PENDING;
    }

    public void update(String name, String description, String address,
                        Integer luggageCount, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.luggageCount = luggageCount;
        this.startTime = startTime;
        this.endTime = endTime;
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
