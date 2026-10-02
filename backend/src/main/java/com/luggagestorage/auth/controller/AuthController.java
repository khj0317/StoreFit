package com.luggagestorage.auth.controller;

import com.luggagestorage.auth.dto.DemoLoginRequest;
import com.luggagestorage.auth.dto.LoginRequest;
import com.luggagestorage.auth.dto.LoginResponse;
import com.luggagestorage.auth.dto.RefreshRequest;
import com.luggagestorage.auth.security.JwtTokenProvider;
import com.luggagestorage.auth.service.LoginAttemptService;
import com.luggagestorage.auth.service.RefreshTokenService;
import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.demo.DemoAccounts;
import com.luggagestorage.demo.DemoService;
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
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final DemoService demoService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        loginAttemptService.checkNotLocked(request.username());
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(request.username());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        loginAttemptService.recordSuccess(request.username());

        Member member = memberRepository.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        return ResponseEntity.ok(tokensFor(member, refreshTokenService.issue(member)));
    }

    /** 휴대폰 인증 없이 둘러볼 수 있는 체험 계정으로 로그인한다 */
    @PostMapping("/demo-login")
    public ResponseEntity<LoginResponse> demoLogin(@Valid @RequestBody DemoLoginRequest request) {
        Member member = demoService.demoMember(request.role());
        return ResponseEntity.ok(tokensFor(member, refreshTokenService.issue(member)));
    }

    /** 액세스 토큰이 만료되면 리프레시 토큰으로 새 토큰 한 쌍을 받는다 */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(request.refreshToken());
        return ResponseEntity.ok(tokensFor(rotated.member(), rotated.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        refreshTokenService.revoke(request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
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

    private LoginResponse tokensFor(Member member, String refreshToken) {
        String accessToken = jwtTokenProvider.createAccessToken(member.getUsername(), member.getRole().name());
        return LoginResponse.of(accessToken, refreshToken, member.getUsername(), member.getName(), member.getRole(),
            DemoAccounts.isDemo(member));
    }
}
