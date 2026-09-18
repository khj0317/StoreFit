package com.luggagestorage.store.dto;

import com.luggagestorage.review.dto.ReviewSummary;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;

import java.time.LocalDateTime;
import java.util.List;

public record StoreResponse(
    Long id,
    Long memberId,
    String memberName,
    String name,
    String description,
    String address,
    List<String> imageUrls,
    Integer luggageCount,
    LocalDateTime startTime,
    LocalDateTime endTime,
    StoreStatus status,
    ReviewSummary review,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static StoreResponse of(Store store, List<String> imageUrls, ReviewSummary review) {
        return new StoreResponse(
            store.getId(),
            store.getMember().getId(),
            store.getMember().getName(),
            store.getName(),
            store.getDescription(),
            store.getAddress(),
            imageUrls,
            store.getLuggageCount(),
            store.getStartTime(),
            store.getEndTime(),
            store.getStatus(),
            review,
            store.getCreatedAt(),
            store.getUpdatedAt()
        );
    }
}
