package com.luggagestorage.verification;

import com.luggagestorage.verification.dto.PhoneCodeRequests;
import com.luggagestorage.verification.dto.SendCodeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** /api/auth/** 아래라 로그인 없이 쓸 수 있다 (번호 변경 인증도 같은 API를 쓴다) */
@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/send")
    public ResponseEntity<SendCodeResponse> send(@Valid @RequestBody PhoneCodeRequests.Send request, HttpServletRequest http) {
        return ResponseEntity.ok(phoneVerificationService.sendCode(request.phoneNumber(), request.purpose(), ClientIp.of(http)));
    }

    @PostMapping("/verify")
    public ResponseEntity<PhoneCodeRequests.Verified> verify(@Valid @RequestBody PhoneCodeRequests.Verify request) {
        String token = phoneVerificationService.verifyCode(request.phoneNumber(), request.purpose(), request.code());
        return ResponseEntity.ok(new PhoneCodeRequests.Verified(token));
    }
}
