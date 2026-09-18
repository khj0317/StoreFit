package com.luggagestorage.store.dto;

import com.luggagestorage.review.dto.ReviewSummary;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
import com.luggagestorage.store.entity.StoreStatus;

import java.time.LocalDate;
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
    StoreCategory category,
    Integer luggageCount,
    LocalDate startDate,
    LocalDate endDate,
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
            store.getCategory(),
            store.getLuggageCount(),
            store.getStartDate(),
            store.getEndDate(),
            store.getStatus(),
            review,
            store.getCreatedAt(),
            store.getUpdatedAt()
        );
    }
}
