package com.luggagestorage.review.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.reservation.entity.Reservation;
import com.luggagestorage.reservation.entity.ReservationStatus;
import com.luggagestorage.reservation.repository.ReservationRepository;
import com.luggagestorage.review.dto.ReviewCreateRequest;
import com.luggagestorage.review.dto.ReviewResponse;
import com.luggagestorage.review.entity.Review;
import com.luggagestorage.review.repository.ReviewRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request) {
        Member member = getCurrentMember();
        Reservation reservation = reservationRepository.findById(request.reservationId())
            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.isOwnedBy(member.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.RESERVATION_NOT_COMPLETED);
        }
        if (reviewRepository.existsByReservation(reservation)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        Review review = new Review(reservation, member, reservation.getStore(), request.rating(), request.content());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    public List<ReviewResponse> getStoreReviews(Long storeId) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        return reviewRepository.findByStoreOrderByCreatedAtDesc(store).stream()
            .map(ReviewResponse::from)
            .toList();
    }

    private Member getCurrentMember() {
        String email = SecurityUtil.getCurrentMemberEmail();
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
