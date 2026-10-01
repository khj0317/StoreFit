package com.luggagestorage.place.entity;

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

/**
 * 스토어핏이 정식 등록한 지점. 이름·주소는 본사가 정하고, 운영자(OWNER)는 아직 주인이 없는
 * 지점을 맡아(claim) 수용량과 소개만 정한다. 운영자가 없는 지점은 체크인할 사람이 없으므로
 * 이용자 예약 목록에 나오지 않는다.
 * capacity는 하루에 동시에 맡을 수 있는 짐 개수다. 예약을 만들 때 이 행을 비관적 락으로 잡아서,
 * 같은 지점에 대한 예약이 한 줄로 처리되게 한다.
 */
@Entity
@Table(name = "storage_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoragePlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 정식 지점 코드 (예: HONGDAE) */
    @Column(unique = true, length = 30)
    private String code;

    /** 운영자. 아직 아무도 맡지 않은 지점이면 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Member owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer capacity;

    /** 지도 표시용 좌표 (없으면 지도에 나오지 않을 뿐 예약은 된다) */
    private Double latitude;

    private Double longitude;

    public StoragePlace(Member owner, String name, String address, String description, Integer capacity) {
        this.owner = owner;
        this.name = name;
        this.address = address;
        this.description = description;
        this.capacity = capacity;
    }

    /** 운영자가 아직 없는 정식 지점 */
    public static StoragePlace officialBranch(String code, String name, String address, String description, Integer capacity) {
        StoragePlace place = new StoragePlace(null, name, address, description, capacity);
        place.code = code;
        return place;
    }

    public void locate(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * 관리자가 본사 등록 정보를 고친다. 운영 중인 지점의 수용량은 운영자가 정하므로
     * 운영자가 없는 지점에서만 기본 수용량을 바꾼다.
     */
    public void updateOfficialInfo(String code, String name, String address, String description, Integer capacity,
                                   Double latitude, Double longitude) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.description = description;
        if (!isOperating()) {
            this.capacity = capacity;
        }
        locate(latitude, longitude);
    }

    public void claim(Member owner, Integer capacity, String description) {
        this.owner = owner;
        updateOperation(capacity, description);
    }

    /** 운영자가 바꿀 수 있는 건 수용량과 소개뿐이다 (이름·주소는 본사 등록 정보) */
    public void updateOperation(Integer capacity, String description) {
        this.capacity = capacity;
        if (description != null) {
            this.description = description;
        }
    }

    public boolean isOperating() {
        return this.owner != null;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.owner != null && this.owner.getId().equals(memberId);
    }
}
