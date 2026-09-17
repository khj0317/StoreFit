package com.luggagestorage.store.dto;

import com.luggagestorage.store.entity.Store;

public record StoreSummaryResponse(
    Long id,
    String name,
    String address,
    Integer pricePerHour,
    Integer capacity,
    String thumbnailUrl
) {

    public static StoreSummaryResponse of(Store store, String thumbnailUrl) {
        return new StoreSummaryResponse(
            store.getId(),
            store.getName(),
            store.getAddress(),
            store.getPricePerHour(),
            store.getCapacity(),
            thumbnailUrl
        );
    }
}
