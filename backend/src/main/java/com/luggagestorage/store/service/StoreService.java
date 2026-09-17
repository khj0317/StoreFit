package com.luggagestorage.store.service;

import com.luggagestorage.auth.security.SecurityUtil;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.store.dto.StoreCreateRequest;
import com.luggagestorage.store.dto.StoreResponse;
import com.luggagestorage.store.dto.StoreSummaryResponse;
import com.luggagestorage.store.dto.StoreUpdateRequest;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreImage;
import com.luggagestorage.store.repository.StoreImageRepository;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public StoreResponse createStore(StoreCreateRequest request) {
        Member host = getCurrentMember();

        Store store = new Store(
            host,
            request.name(),
            request.description(),
            request.address(),
            request.latitude(),
            request.longitude(),
            request.pricePerHour(),
            request.capacity(),
            request.openTime(),
            request.closeTime()
        );
        storeRepository.save(store);

        List<String> imageUrls = saveImages(store, request.imageUrls());
        return StoreResponse.of(store, imageUrls);
    }

    public List<StoreSummaryResponse> getStores() {
        return storeRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(store -> StoreSummaryResponse.of(store, findThumbnailUrl(store)))
            .toList();
    }

    public List<StoreSummaryResponse> getMyStores() {
        Member host = getCurrentMember();
        return storeRepository.findByHostOrderByCreatedAtDesc(host).stream()
            .map(store -> StoreSummaryResponse.of(store, findThumbnailUrl(store)))
            .toList();
    }

    public StoreResponse getStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        List<String> imageUrls = storeImageRepository.findByStoreOrderBySortOrderAsc(store).stream()
            .map(StoreImage::getImageUrl)
            .toList();
        return StoreResponse.of(store, imageUrls);
    }

    @Transactional
    public StoreResponse updateStore(Long storeId, StoreUpdateRequest request) {
        Store store = getStoreOrThrow(storeId);
        Member current = getCurrentMember();
        if (!store.isOwnedBy(current.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        store.update(
            request.name(),
            request.description(),
            request.address(),
            request.latitude(),
            request.longitude(),
            request.pricePerHour(),
            request.capacity(),
            request.openTime(),
            request.closeTime()
        );

        List<String> imageUrls;
        if (request.imageUrls() != null) {
            storeImageRepository.deleteByStore(store);
            imageUrls = saveImages(store, request.imageUrls());
        } else {
            imageUrls = storeImageRepository.findByStoreOrderBySortOrderAsc(store).stream()
                .map(StoreImage::getImageUrl)
                .toList();
        }

        return StoreResponse.of(store, imageUrls);
    }

    @Transactional
    public void deleteStore(Long storeId) {
        Store store = getStoreOrThrow(storeId);
        Member current = getCurrentMember();
        if (!store.isOwnedBy(current.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        storeImageRepository.deleteByStore(store);
        storeRepository.delete(store);
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

    private String findThumbnailUrl(Store store) {
        return storeImageRepository.findByStoreOrderBySortOrderAsc(store).stream()
            .min(Comparator.comparing(StoreImage::getSortOrder))
            .map(StoreImage::getImageUrl)
            .orElse(null);
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
