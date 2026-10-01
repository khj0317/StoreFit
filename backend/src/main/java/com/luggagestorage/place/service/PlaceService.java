package com.luggagestorage.place.service;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.place.dto.PlaceResponse;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import com.luggagestorage.store.entity.Store;
import com.luggagestorage.store.entity.StoreStatus;
import com.luggagestorage.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 이용자가 예약할 보관소를 고를 때 쓰는 조회 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final StoragePlaceRepository storagePlaceRepository;
    private final StoreRepository storeRepository;

    /** 운영 중인 지점만 돌려준다. 기간을 주면 지점마다 그 기간에 남은 자리 수를 함께 돌려준다 */
    public List<PlaceResponse> getPlaces(LocalDate startDate, LocalDate endDate) {
        List<StoragePlace> places = storagePlaceRepository.findByOwnerIsNotNullOrderByIdAsc();
        if (places.isEmpty()) {
            return List.of();
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return places.stream().map(place -> PlaceResponse.of(place, null)).toList();
        }

        Map<Long, List<Store>> overlappingByPlace = storeRepository
            .findOverlapping(places, StoreStatus.ACTIVE, startDate, endDate).stream()
            .collect(Collectors.groupingBy(store -> store.getPlace().getId()));

        return places.stream()
            .map(place -> {
                int peak = CapacityCalculator.peakOccupancy(
                    overlappingByPlace.getOrDefault(place.getId(), List.of()), startDate, endDate);
                return PlaceResponse.of(place, Math.max(0, place.getCapacity() - peak));
            })
            .toList();
    }

    public PlaceResponse getPlace(Long placeId) {
        StoragePlace place = storagePlaceRepository.findById(placeId)
            .filter(StoragePlace::isOperating)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        return PlaceResponse.of(place, null);
    }
}
