package com.luggagestorage.store.dto;

import com.luggagestorage.store.entity.Store;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record StoreResponse(
    Long id,
    Long hostId,
    String hostName,
    String name,
    String description,
    String address,
    Double latitude,
    Double longitude,
    Integer pricePerHour,
    Integer capacity,
    LocalTime openTime,
    LocalTime closeTime,
    List<String> imageUrls,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static StoreResponse of(Store store, List<String> imageUrls) {
        return new StoreResponse(
            store.getId(),
            store.getHost().getId(),
            store.getHost().getName(),
            store.getName(),
            store.getDescription(),
            store.getAddress(),
            store.getLatitude(),
            store.getLongitude(),
            store.getPricePerHour(),
            store.getCapacity(),
            store.getOpenTime(),
            store.getCloseTime(),
            imageUrls,
            store.getCreatedAt(),
            store.getUpdatedAt()
        );
    }
}
