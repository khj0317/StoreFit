package com.luggagestorage.reservation.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.reservation.dto.ReservationCreateRequest;
import com.luggagestorage.reservation.dto.ReservationResponse;
import com.luggagestorage.reservation.entity.Reservation;
import com.luggagestorage.reservation.entity.ReservationStatus;
import com.luggagestorage.reservation.repository.ReservationRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        Member member = getCurrentMember();
        Store store = storeRepository.findById(request.storeId())
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        if (!request.endTime().isAfter(request.startTime())) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_TIME);
        }

        Reservation reservation = new Reservation(
            member,
            store,
            request.luggageCount(),
            request.startTime(),
            request.endTime()
        );

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    public List<ReservationResponse> getMyReservations() {
        Member member = getCurrentMember();
        return reservationRepository.findByMemberOrderByCreatedAtDesc(member).stream()
            .map(ReservationResponse::from)
            .toList();
    }

    public ReservationResponse getReservation(Long reservationId) {
        Reservation reservation = getReservationOrThrow(reservationId);
        Member current = getCurrentMember();
        if (!reservation.isOwnedBy(current.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse completeReservation(Long reservationId) {
        Reservation reservation = getReservationOrThrow(reservationId);
        requireOwner(reservation);
        requireStatus(reservation, ReservationStatus.PENDING);

        reservation.complete();
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        Reservation reservation = getReservationOrThrow(reservationId);
        requireOwner(reservation);
        requireStatus(reservation, ReservationStatus.PENDING);

        reservation.cancel();
        return ReservationResponse.from(reservation);
    }

    private void requireOwner(Reservation reservation) {
        Member current = getCurrentMember();
        if (!reservation.isOwnedBy(current.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void requireStatus(Reservation reservation, ReservationStatus expected) {
        if (reservation.getStatus() != expected) {
            throw new BusinessException(ErrorCode.INVALID_RESERVATION_STATUS);
        }
    }

    private Reservation getReservationOrThrow(Long reservationId) {
        return reservationRepository.findById(reservationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
    }

    private Member getCurrentMember() {
        String email = SecurityUtil.getCurrentMemberEmail();
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
