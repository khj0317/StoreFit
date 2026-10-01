package com.luggagestorage.place.entity;

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

/** 운영자의 지점 운영 신청. 관리자가 승인하면 그 운영자가 지점을 맡는다 */
@Entity
@Table(name = "branch_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BranchApplication extends BaseTimeEntity {

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private StoragePlace place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Member applicant;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 500)
    private String description;

    /** 신청할 때 관리자에게 남기는 말 (경력, 매장 상황 등) */
    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    private LocalDateTime decidedAt;

    @Column(length = 300)
    private String rejectReason;

    public BranchApplication(StoragePlace place, Member applicant, Integer capacity, String description, String message) {
        this.place = place;
        this.applicant = applicant;
        this.capacity = capacity;
        this.description = description;
        this.message = message;
        this.status = Status.PENDING;
    }

    public boolean isPending() {
        return status == Status.PENDING;
    }

    public void approve(LocalDateTime now) {
        this.status = Status.APPROVED;
        this.decidedAt = now;
    }

    public void reject(String reason, LocalDateTime now) {
        this.status = Status.REJECTED;
        this.rejectReason = reason;
        this.decidedAt = now;
    }
}
