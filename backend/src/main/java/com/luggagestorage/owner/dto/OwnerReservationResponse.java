package com.luggagestorage.owner.dto;

import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreCategory;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.service.OverduePolicy;
import com.luggagestorage.verification.PhoneNumbers;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 운영자가 보는 예약 한 건.
 * @param nextAction 지금 이 예약에 할 수 있는 처리 (QR을 스캔했을 때 어떤 버튼을 보여줄지)
 */
public record OwnerReservationResponse(
    Long id,
    String checkInCode,
    Long placeId,
    String placeName,
    String customerName,
    String customerPhone,
    String name,
    StoreCategory category,
    Integer luggageCount,
    LocalDate startDate,
    LocalDate endDate,
    StoreStatus status,
    boolean paid,
    Integer totalPrice,
    LocalDateTime checkedInAt,
    LocalDateTime checkedOutAt,
    long overdueDays,
    long overdueFee,
    NextAction nextAction
) {

    public enum NextAction {
        CHECK_IN,
        CHECK_OUT,
        NONE
    }

    public static OwnerReservationResponse of(Store store, boolean paid, NextAction nextAction, LocalDate today) {
        boolean inUse = store.getStatus() == StoreStatus.IN_USE;
        return new OwnerReservationResponse(
            store.getId(),
            store.getCheckInCode(),
            store.getPlace().getId(),
            store.getPlace().getName(),
            store.getMember().getName(),
            PhoneNumbers.format(store.getMember().getPhoneNumber()),
            store.getName(),
            store.getCategory(),
            store.getLuggageCount(),
            store.getStartDate(),
            store.getEndDate(),
            store.getStatus(),
            paid,
            store.getTotalPrice(),
            store.getCheckedInAt(),
            store.getCheckedOutAt(),
            inUse ? OverduePolicy.overdueDays(store, today) : 0,
            inUse ? OverduePolicy.overdueFee(store, today) : 0,
            nextAction
        );
    }

    /** 체크아웃한 뒤에도 방금 받은 연체료를 화면에 보여주기 위해 */
    public OwnerReservationResponse withOverdue(long days, long fee) {
        return new OwnerReservationResponse(id, checkInCode, placeId, placeName, customerName, customerPhone, name, category,
            luggageCount, startDate, endDate, status, paid, totalPrice, checkedInAt, checkedOutAt, days, fee, nextAction);
    }
}
