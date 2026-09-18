package com.luggagestorage.auth.controller;

import com.luggagestorage.auth.dto.LoginRequest;
import com.luggagestorage.auth.dto.LoginResponse;
import com.luggagestorage.auth.security.JwtTokenProvider;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.dto.FindUsernameRequest;
import com.luggagestorage.member.dto.FindUsernameResponse;
import com.luggagestorage.member.dto.ResetPasswordRequest;
import com.luggagestorage.member.dto.SignupRequest;
import com.luggagestorage.member.dto.SignupResponse;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import com.luggagestorage.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Member member = memberRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        String accessToken = jwtTokenProvider.createAccessToken(member.getUsername(), member.getRole().name());
        return ResponseEntity.ok(LoginResponse.of(accessToken, member.getUsername(), member.getName()));
    }

    @PostMapping("/find-username")
    public ResponseEntity<FindUsernameResponse> findUsername(@Valid @RequestBody FindUsernameRequest request) {
        return ResponseEntity.ok(memberService.findUsername(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        memberService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
