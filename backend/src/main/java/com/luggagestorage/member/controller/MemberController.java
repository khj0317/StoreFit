package com.luggagestorage.member.controller;

import com.luggagestorage.member.dto.ChangePasswordRequest;
import com.luggagestorage.member.dto.DeleteAccountRequest;
import com.luggagestorage.member.dto.MemberProfileResponse;
import com.luggagestorage.member.dto.UpdateProfileRequest;
import com.luggagestorage.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<MemberProfileResponse> getProfile() {
        return ResponseEntity.ok(memberService.getMyProfile());
    }

    @PatchMapping
    public ResponseEntity<MemberProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(memberService.updateProfile(request));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        memberService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        memberService.deleteAccount(request);
        return ResponseEntity.noContent().build();
    }
}
