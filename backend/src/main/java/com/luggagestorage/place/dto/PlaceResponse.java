package com.luggagestorage.place.dto;

import com.luggagestorage.place.entity.StoragePlace;

/**
 * @param remaining 조회한 기간에 더 맡길 수 있는 짐 개수. 기간을 주지 않고 조회하면 null
 */
public record PlaceResponse(
    Long id,
    String name,
    String address,
    String description,
    Integer capacity,
    Integer remaining,
    Double latitude,
    Double longitude
) {

    public static PlaceResponse of(StoragePlace place, Integer remaining) {
        return new PlaceResponse(
            place.getId(),
            place.getName(),
            place.getAddress(),
            place.getDescription(),
            place.getCapacity(),
            remaining,
            place.getLatitude(),
            place.getLongitude()
        );
    }
}
