package com.luggagestorage.review.dto;

import com.luggagestorage.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewSummary(
    Long id,
    Integer rating,
    String content,
    LocalDateTime createdAt
) {

    public static ReviewSummary from(Review review) {
        return new ReviewSummary(review.getId(), review.getRating(), review.getContent(), review.getCreatedAt());
    }
}
