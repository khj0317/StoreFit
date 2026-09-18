package com.luggagestorage.member.service;

import com.luggagestorage.common.exception.BusinessException;
import com.luggagestorage.common.exception.ErrorCode;
import com.luggagestorage.member.dto.FindUsernameRequest;
import com.luggagestorage.member.dto.FindUsernameResponse;
import com.luggagestorage.member.dto.ResetPasswordRequest;
import com.luggagestorage.member.dto.SignupRequest;
import com.luggagestorage.member.dto.SignupResponse;
import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        String email = normalize(request.email());
        if (email != null && memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.createUser(
            request.username(),
            passwordEncoder.encode(request.password()),
            request.name(),
            email,
            request.phoneNumber()
        );

        return SignupResponse.from(memberRepository.save(member));
    }

    public FindUsernameResponse findUsername(FindUsernameRequest request) {
        Member member = memberRepository.findByEmail(request.email())
            .filter(candidate -> candidate.getName().equals(request.name()))
            .orElseThrow(() -> new BusinessException(ErrorCode.IDENTITY_NOT_VERIFIED));

        return new FindUsernameResponse(member.getUsername());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Member member = memberRepository.findByUsername(request.username())
            .filter(candidate -> request.email().equals(candidate.getEmail()))
            .filter(candidate -> candidate.getName().equals(request.name()))
            .orElseThrow(() -> new BusinessException(ErrorCode.IDENTITY_NOT_VERIFIED));

        member.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
