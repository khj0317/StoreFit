package com.luggagestorage.store.entity;

import com.luggagestorage.common.entity.BaseTimeEntity;
import com.luggagestorage.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Member host;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String address;

    public Store(Member host, String name, String description, String address) {
        this.host = host;
        this.name = name;
        this.description = description;
        this.address = address;
    }

    public void update(String name, String description, String address) {
        this.name = name;
        this.description = description;
        this.address = address;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.host.getId().equals(memberId);
    }
}
