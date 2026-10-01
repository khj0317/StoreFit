package com.luggagestorage.owner.controller;

import com.luggagestorage.owner.dto.OwnerDashboardResponse;
import com.luggagestorage.owner.dto.OwnerReservationResponse;
import com.luggagestorage.owner.service.OwnerService;
import com.luggagestorage.place.dto.BranchApplicationResponse;
import com.luggagestorage.place.dto.BranchApplyRequest;
import com.luggagestorage.place.dto.PlaceRequest;
import com.luggagestorage.place.dto.PlaceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/owner/** 는 SecurityConfig에서 OWNER 역할만 들어올 수 있다 */
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    @GetMapping("/dashboard")
    public ResponseEntity<OwnerDashboardResponse> getDashboard() {
        return ResponseEntity.ok(ownerService.getDashboard());
    }

    @GetMapping("/places")
    public ResponseEntity<List<PlaceResponse>> getMyPlaces() {
        return ResponseEntity.ok(ownerService.getMyPlaces());
    }

    /** 아직 운영자가 없는 정식 지점 (운영자가 맡을 지점을 고르는 목록) */
    @GetMapping("/branches")
    public ResponseEntity<List<PlaceResponse>> getAvailableBranches() {
        return ResponseEntity.ok(ownerService.getAvailableBranches());
    }

    /** 지점 운영 신청 (본사 관리자가 승인하면 운영 시작) */
    @PostMapping("/branches/{placeId}/applications")
    public ResponseEntity<BranchApplicationResponse> applyForBranch(
        @PathVariable Long placeId,
        @Valid @RequestBody BranchApplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ownerService.applyForBranch(placeId, request));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<BranchApplicationResponse>> getMyApplications() {
        return ResponseEntity.ok(ownerService.getMyApplications());
    }

    @PutMapping("/places/{placeId}")
    public ResponseEntity<PlaceResponse> updatePlace(@PathVariable Long placeId, @Valid @RequestBody PlaceRequest request) {
        return ResponseEntity.ok(ownerService.updatePlace(placeId, request));
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<OwnerReservationResponse>> getReservations() {
        return ResponseEntity.ok(ownerService.getReservations());
    }

    @GetMapping("/check-ins/{code}")
    public ResponseEntity<OwnerReservationResponse> lookup(@PathVariable String code) {
        return ResponseEntity.ok(ownerService.lookup(code));
    }

    @PostMapping("/check-ins/{code}/check-in")
    public ResponseEntity<OwnerReservationResponse> checkIn(@PathVariable String code) {
        return ResponseEntity.ok(ownerService.checkIn(code));
    }

    @PostMapping("/check-ins/{code}/check-out")
    public ResponseEntity<OwnerReservationResponse> checkOut(@PathVariable String code) {
        return ResponseEntity.ok(ownerService.checkOut(code));
    }
}
