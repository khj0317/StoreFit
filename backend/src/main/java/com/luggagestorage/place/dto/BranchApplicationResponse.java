package com.luggagestorage.place.dto;

import com.luggagestorage.place.entity.BranchApplication;
import com.luggagestorage.verification.PhoneNumbers;

import java.time.LocalDateTime;

public record BranchApplicationResponse(
    Long id,
    Long placeId,
    String placeName,
    String placeAddress,
    Long applicantId,
    String applicantName,
    String applicantPhone,
    Integer capacity,
    String description,
    String message,
    BranchApplication.Status status,
    String rejectReason,
    LocalDateTime createdAt,
    LocalDateTime decidedAt
) {

    public static BranchApplicationResponse from(BranchApplication application) {
        return new BranchApplicationResponse(
            application.getId(),
            application.getPlace().getId(),
            application.getPlace().getName(),
            application.getPlace().getAddress(),
            application.getApplicant().getId(),
            application.getApplicant().getName(),
            PhoneNumbers.format(application.getApplicant().getPhoneNumber()),
            application.getCapacity(),
            application.getDescription(),
            application.getMessage(),
            application.getStatus(),
            application.getRejectReason(),
            application.getCreatedAt(),
            application.getDecidedAt()
        );
    }
}
