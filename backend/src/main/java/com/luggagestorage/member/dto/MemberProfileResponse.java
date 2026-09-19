package com.luggagestorage.member.dto;

import com.luggagestorage.member.entity.Member;

public record MemberProfileResponse(Long id, String username, String name, String email, String phoneNumber) {

    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
            member.getId(),
            member.getUsername(),
            member.getName(),
            member.getEmail(),
            member.getPhoneNumber()
        );
    }
}
