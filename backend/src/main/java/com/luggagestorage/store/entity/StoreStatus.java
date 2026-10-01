package com.luggagestorage.store.entity;

import java.util.List;

/**
 * 예약 → (결제) → 운영자 QR 체크인 → 보관중 → 운영자 QR 체크아웃 → 완료.
 * 결제했는지는 Payment가 따로 들고 있고, 보관 단계 전환은 운영자만 할 수 있다.
 */
public enum StoreStatus {
    /** 예약됨. 결제 전이거나, 결제했지만 아직 짐을 맡기지 않은 상태 */
    PENDING,
    /** 운영자가 체크인 QR을 스캔해서 짐을 받아 보관 중 */
    IN_USE,
    /** 운영자가 체크아웃 QR을 스캔해서 짐을 돌려줌 */
    COMPLETED,
    /** 이용자가 결제한 예약을 취소함 (환불 처리) */
    CANCELED,
    /** 결제 마감(예약 후 30분)까지 결제하지 않아 자동 취소됨 */
    EXPIRED,
    /** 결제했지만 보관 시작일이 지나도록 체크인하지 않음 (환불 없음) */
    NO_SHOW;

    /** 보관소 자리를 차지하는 상태 */
    public static final List<StoreStatus> ACTIVE = List.of(PENDING, IN_USE);
}
