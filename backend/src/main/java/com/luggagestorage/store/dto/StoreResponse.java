package com.luggagestorage.store.dto;

import com.luggagestorage.payment.entity.Payment;
import com.luggagestorage.payment.entity.PaymentStatus;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.service.OverduePolicy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param checkInCode 결제한 예약에만 있다. 이용자 본인에게만 내려주는 응답이라 그대로 담는다
 * @param refundedAmount 취소로 환불된 금액 (취소하지 않았으면 0)
 */
public record StoreResponse(
    Long id,
    Long memberId,
    String memberName,
    Long placeId,
    String placeName,
    String placeAddress,
    String name,
    String description,
    List<String> imageUrls,
    StoreCategory category,
    Integer luggageCount,
    LocalDate startDate,
    LocalDate endDate,
    StoreStatus status,
    Integer totalPrice,
    PaymentStatus paymentStatus,
    Integer refundedAmount,
    String checkInCode,
    LocalDateTime checkedInAt,
    LocalDateTime checkedOutAt,
    LocalDateTime paymentDeadline,
    long overdueDays,
    long overdueFee,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static StoreResponse of(Store store, List<String> imageUrls, Payment payment, LocalDate today) {
        boolean inUse = store.getStatus() == StoreStatus.IN_USE;
        return new StoreResponse(
            store.getId(),
            store.getMember().getId(),
            store.getMember().getName(),
            store.getPlace().getId(),
            store.getPlace().getName(),
            store.getPlace().getAddress(),
            store.getName(),
            store.getDescription(),
            imageUrls,
            store.getCategory(),
            store.getLuggageCount(),
            store.getStartDate(),
            store.getEndDate(),
            store.getStatus(),
            store.getTotalPrice(),
            payment == null ? null : payment.getStatus(),
            payment == null ? 0 : payment.getCanceledAmount(),
            store.getCheckInCode(),
            store.getCheckedInAt(),
            store.getCheckedOutAt(),
            store.getPaymentDeadline(),
            inUse ? OverduePolicy.overdueDays(store, today) : 0,
            inUse ? OverduePolicy.overdueFee(store, today) : 0,
            store.getCreatedAt(),
            store.getUpdatedAt()
        );
    }
}
