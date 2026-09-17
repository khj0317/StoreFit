package com.luggagestorage.store.dto;

import com.luggagestorage.store.entity.Store;

import java.time.LocalDateTime;
import java.util.List;

public record StoreResponse(
    Long id,
    Long hostId,
    String hostName,
    String name,
    String description,
    String address,
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
            imageUrls,
            store.getCreatedAt(),
            store.getUpdatedAt()
        );
    }
}
