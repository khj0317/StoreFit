package com.luggagestorage.review.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.review.dto.ReviewCreateRequest;
import com.luggagestorage.review.dto.ReviewSummary;
import com.luggagestorage.review.entity.Review;
import com.luggagestorage.review.repository.ReviewRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReviewSummary createReview(ReviewCreateRequest request) {
        Member member = getCurrentMember();
        Store store = storeRepository.findById(request.storeId())
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));

        if (!store.isOwnedBy(member.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        if (store.getStatus() != StoreStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.STORE_NOT_COMPLETED);
        }
        if (reviewRepository.findByStore(store).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        Review review = new Review(store, request.rating(), request.content());
        return ReviewSummary.from(reviewRepository.save(review));
    }

    private Member getCurrentMember() {
        String username = SecurityUtil.getCurrentUsername();
        return memberRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
