package com.luggagestorage.admin.controller;

import com.luggagestorage.admin.dto.AdminBranchResponse;
import com.luggagestorage.admin.dto.AdminNotificationResponse;
import com.luggagestorage.admin.service.AdminService;
import com.luggagestorage.place.dto.BranchApplicationResponse;
import com.luggagestorage.place.dto.BranchRequest;
import com.luggagestorage.place.entity.BranchApplication;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** /api/admin/** 는 SecurityConfig에서 ADMIN 역할만 들어올 수 있다 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/branches")
    public ResponseEntity<List<AdminBranchResponse>> getBranches() {
        return ResponseEntity.ok(adminService.getBranches());
    }

    @PostMapping("/branches")
    public ResponseEntity<AdminBranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createBranch(request));
    }

    @PutMapping("/branches/{placeId}")
    public ResponseEntity<AdminBranchResponse> updateBranch(@PathVariable Long placeId, @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.ok(adminService.updateBranch(placeId, request));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<BranchApplicationResponse>> getApplications(
        @RequestParam(required = false) BranchApplication.Status status
    ) {
        return ResponseEntity.ok(adminService.getApplications(status));
    }

    @PostMapping("/applications/{applicationId}/approve")
    public ResponseEntity<BranchApplicationResponse> approve(@PathVariable Long applicationId) {
        return ResponseEntity.ok(adminService.approve(applicationId));
    }

    public record RejectRequest(String reason) {
    }

    @PostMapping("/applications/{applicationId}/reject")
    public ResponseEntity<BranchApplicationResponse> reject(
        @PathVariable Long applicationId,
        @RequestBody(required = false) RejectRequest request
    ) {
        return ResponseEntity.ok(adminService.reject(applicationId, request == null ? null : request.reason()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<AdminNotificationResponse>> getNotifications() {
        return ResponseEntity.ok(adminService.getRecentNotifications());
    }
}
