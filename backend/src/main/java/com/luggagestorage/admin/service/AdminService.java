package com.luggagestorage.admin.service;

import com.luggagestorage.admin.dto.AdminBranchResponse;
import com.luggagestorage.admin.dto.AdminNotificationResponse;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.notification.NotificationRepository;
import com.luggagestorage.notification.NotificationService;
import com.luggagestorage.notification.NotificationType;
import com.luggagestorage.place.dto.BranchApplicationResponse;
import com.luggagestorage.place.dto.BranchRequest;
import com.luggagestorage.place.entity.BranchApplication;
import com.luggagestorage.place.entity.StoragePlace;
import com.luggagestorage.place.repository.BranchApplicationRepository;
import com.luggagestorage.place.repository.StoragePlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 본사 관리자 기능: 정식 지점 등록·수정, 운영 신청 심사, 알림 발송 기록 확인 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private static final String AUTO_REJECT_REASON = "다른 운영자가 이 지점을 맡게 되었습니다.";

    private final StoragePlaceRepository storagePlaceRepository;
    private final BranchApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    // ───────── 정식 지점 ─────────

    public List<AdminBranchResponse> getBranches() {
        Map<Long, Long> pending = applicationRepository.findByStatus(BranchApplication.Status.PENDING).stream()
            .collect(Collectors.groupingBy(application -> application.getPlace().getId(), Collectors.counting()));
        return storagePlaceRepository.findAll().stream()
            .sorted((a, b) -> Long.compare(a.getId(), b.getId()))
            .map(place -> AdminBranchResponse.of(place, pending.getOrDefault(place.getId(), 0L)))
            .toList();
    }

    @Transactional
    public AdminBranchResponse createBranch(BranchRequest request) {
        if (storagePlaceRepository.findByCode(request.code()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_BRANCH_CODE);
        }
        StoragePlace place = StoragePlace.officialBranch(
            request.code(), request.name(), request.address(), blankToNull(request.description()), request.capacity());
        place.locate(request.latitude(), request.longitude());
        return AdminBranchResponse.of(storagePlaceRepository.save(place), 0);
    }

    @Transactional
    public AdminBranchResponse updateBranch(Long placeId, BranchRequest request) {
        StoragePlace place = storagePlaceRepository.findByIdForUpdate(placeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        storagePlaceRepository.findByCode(request.code())
            .filter(other -> !other.getId().equals(placeId))
            .ifPresent(other -> {
                throw new BusinessException(ErrorCode.DUPLICATE_BRANCH_CODE);
            });
        place.updateOfficialInfo(request.code(), request.name(), request.address(), blankToNull(request.description()),
            request.capacity(), request.latitude(), request.longitude());
        return AdminBranchResponse.of(place, applicationRepository.findByPlaceAndStatus(place, BranchApplication.Status.PENDING).size());
    }

    // ───────── 운영 신청 심사 ─────────

    public List<BranchApplicationResponse> getApplications(BranchApplication.Status status) {
        List<BranchApplication> applications = status == null
            ? applicationRepository.findAllWithDetails()
            : applicationRepository.findByStatus(status);
        return applications.stream().map(BranchApplicationResponse::from).toList();
    }

    /**
     * 승인: 지점을 잠근 상태에서 아직 운영자가 없는지 확인하고 맡긴다. 같은 지점에 들어온 다른 신청은
     * 자동으로 반려한다. 두 관리자가 서로 다른 신청을 동시에 승인해도 지점 락 때문에 한 명만 성공한다.
     */
    @Transactional
    public BranchApplicationResponse approve(Long applicationId) {
        Long placeId = applicationRepository.findById(applicationId)
            .map(found -> found.getPlace().getId())
            .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_APPLICATION_NOT_FOUND));
        // 락 순서를 항상 "지점 → 신청"으로 맞춘다. 신청부터 잠그면, 같은 지점의 다른 신청을 승인하는
        // 요청과 서로 상대가 잠근 신청을 기다리며 교착 상태(deadlock)에 빠진다
        StoragePlace place = storagePlaceRepository.findByIdForUpdate(placeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        BranchApplication application = applicationRepository.findByIdForUpdate(applicationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_APPLICATION_NOT_FOUND));
        if (!application.isPending()) {
            throw new BusinessException(ErrorCode.BRANCH_APPLICATION_DECIDED);
        }
        if (place.isOperating()) {
            throw new BusinessException(ErrorCode.BRANCH_ALREADY_TAKEN);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        place.claim(application.getApplicant(), application.getCapacity(), application.getDescription());
        application.approve(now);
        notificationService.notify(application.getApplicant(), NotificationType.BRANCH_APPROVED, Map.of("place", place.getName()));

        for (BranchApplication other : applicationRepository.findByPlaceAndStatus(place, BranchApplication.Status.PENDING)) {
            if (!other.getId().equals(application.getId())) {
                other.reject(AUTO_REJECT_REASON, now);
                notificationService.notify(other.getApplicant(), NotificationType.BRANCH_REJECTED,
                    Map.of("place", place.getName(), "reason", AUTO_REJECT_REASON));
            }
        }
        return BranchApplicationResponse.from(application);
    }

    @Transactional
    public BranchApplicationResponse reject(Long applicationId, String reason) {
        BranchApplication application = applicationRepository.findByIdForUpdate(applicationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BRANCH_APPLICATION_NOT_FOUND));
        if (!application.isPending()) {
            throw new BusinessException(ErrorCode.BRANCH_APPLICATION_DECIDED);
        }
        String finalReason = reason == null || reason.isBlank() ? "운영 조건이 맞지 않습니다." : reason.trim();
        application.reject(finalReason, LocalDateTime.now(clock));
        notificationService.notify(application.getApplicant(), NotificationType.BRANCH_REJECTED,
            Map.of("place", application.getPlace().getName(), "reason", finalReason));
        return BranchApplicationResponse.from(application);
    }

    // ───────── 알림 기록 ─────────

    public List<AdminNotificationResponse> getRecentNotifications() {
        return notificationRepository.findByOrderByIdDesc(PageRequest.of(0, 50)).stream()
            .map(AdminNotificationResponse::from)
            .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
