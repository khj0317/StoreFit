package com.luggagestorage.admin.dto;

import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.verification.PhoneNumbers;

/** 관리자 화면의 지점 한 줄: 본사 정보와 지금 누가 운영하는지 */
public record AdminBranchResponse(
    Long id,
    String code,
    String name,
    String address,
    String description,
    Integer capacity,
    Double latitude,
    Double longitude,
    boolean operating,
    String ownerName,
    String ownerPhone,
    long pendingApplications
) {

    public static AdminBranchResponse of(StoragePlace place, long pendingApplications) {
        return new AdminBranchResponse(
            place.getId(),
            place.getCode(),
            place.getName(),
            place.getAddress(),
            place.getDescription(),
            place.getCapacity(),
            place.getLatitude(),
            place.getLongitude(),
            place.isOperating(),
            place.isOperating() ? place.getOwner().getName() : null,
            place.isOperating() ? PhoneNumbers.format(place.getOwner().getPhoneNumber()) : null,
            pendingApplications
        );
    }
}
