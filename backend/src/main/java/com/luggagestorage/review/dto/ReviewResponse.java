package com.luggagestorage.review.dto;

import com.luggagestorage.review.entity.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
    Long id,
    Long reservationId,
    Long storeId,
    Long memberId,
    String memberName,
    Integer rating,
    String content,
    LocalDateTime createdAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
            review.getId(),
            review.getReservation().getId(),
            review.getStore().getId(),
            review.getMember().getId(),
            review.getMember().getName(),
            review.getRating(),
            review.getContent(),
            review.getCreatedAt()
        );
    }
}
