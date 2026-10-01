package com.luggagestorage.notification;

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

/** 문자·알림톡 발송 기록. 실제 발송은 트랜잭션이 끝난 뒤 비동기로 하고 결과를 여기에 남긴다 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    public enum Status {
        PENDING,
        SENT,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /** 실제로 나간 경로: SMS, ALIMTALK, LOG(개발용) */
    @Column(length = 20)
    private String channel;

    @Column(nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(length = 500)
    private String error;

    private LocalDateTime sentAt;

    public Notification(Member member, String phoneNumber, NotificationType type, String content, LocalDateTime createdAt) {
        this.member = member;
        this.phoneNumber = phoneNumber;
        this.type = type;
        this.content = content;
        this.createdAt = createdAt;
        this.status = Status.PENDING;
    }

    public void markSent(String channel, LocalDateTime sentAt) {
        this.status = Status.SENT;
        this.channel = channel;
        this.sentAt = sentAt;
    }

    public void markFailed(String error) {
        this.status = Status.FAILED;
        this.error = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    }
}
