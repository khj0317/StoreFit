package com.luggagestorage.store.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.review.dto.ReviewSummary;
import com.luggagestorage.review.repository.ReviewRepository;
import com.luggagestorage.store.dto.StoreCreateRequest;
import com.luggagestorage.store.dto.StoreResponse;
import com.luggagestorage.store.dto.StoreUpdateRequest;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreImage;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreImageRepository;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        Member member = getCurrentMember();
        validateTimeRange(request.startTime(), request.endTime());

        Store store = new Store(
            member,
            request.name(),
            request.description(),
            request.address(),
            request.luggageCount(),
            request.startTime(),
            request.endTime()
        );
        storeRepository.save(store);

        List<String> imageUrls = saveImages(store, request.imageUrls());
        return toResponse(store, imageUrls);
    }

    public List<StoreResponse> getMyStores() {
        Member member = getCurrentMember();
        return storeRepository.findByMemberOrderByCreatedAtDesc(member).stream()
            .map(store -> toResponse(store, imageUrlsOf(store)))
            .toList();
    }

    public StoreResponse getStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        return toResponse(store, imageUrlsOf(store));
    }

    @Transactional
    public StoreResponse updateStore(Long storeId, StoreUpdateRequest request) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.PENDING);
        validateTimeRange(request.startTime(), request.endTime());

        store.update(
            request.name(),
            request.description(),
            request.address(),
            request.luggageCount(),
            request.startTime(),
            request.endTime()
        );

        List<String> imageUrls;
        if (request.imageUrls() != null) {
            storeImageRepository.deleteByStore(store);
            imageUrls = saveImages(store, request.imageUrls());
        } else {
            imageUrls = imageUrlsOf(store);
        }

        return toResponse(store, imageUrls);
    }

    @Transactional
    public void deleteStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);

        storeImageRepository.deleteByStore(store);
        storeRepository.delete(store);
    }

    @Transactional
    public StoreResponse completeStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.PENDING);

        store.complete();
        return toResponse(store, imageUrlsOf(store));
    }

    @Transactional
    public StoreResponse cancelStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        requireOwner(store);
        requireStatus(store, StoreStatus.PENDING);

        store.cancel();
        return toResponse(store, imageUrlsOf(store));
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_STORE_TIME);
        }
    }

    private List<String> saveImages(Store store, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }

        List<StoreImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(new StoreImage(store, imageUrls.get(i), i));
        }
        storeImageRepository.saveAll(images);
        return imageUrls;
    }

    private List<String> imageUrlsOf(Store store) {
        return storeImageRepository.findByStoreOrderBySortOrderAsc(store).stream()
            .map(StoreImage::getImageUrl)
            .toList();
    }

    private StoreResponse toResponse(Store store, List<String> imageUrls) {
        ReviewSummary review = reviewRepository.findByStore(store)
            .map(ReviewSummary::from)
            .orElse(null);
        return StoreResponse.of(store, imageUrls, review);
    }

    private void requireOwner(Store store) {
        Member current = getCurrentMember();
        if (!store.isOwnedBy(current.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }

    private void requireStatus(Store store, StoreStatus expected) {
        if (store.getStatus() != expected) {
            throw new BusinessException(ErrorCode.INVALID_STORE_STATUS);
        }
    }

    private Store getStoreOrThrow(Long storeId) {
        return storeRepository.findById(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
    }

    private Member getCurrentMember() {
        String email = SecurityUtil.getCurrentMemberEmail();
        return memberRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
