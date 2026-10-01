package com.luggagestorage.member.dto;

import com.luggagestorage.member.entity.Member;
import com.luggagestorage.member.entity.MemberRole;
import com.luggagestorage.verification.PhoneNumbers;

public record MemberProfileResponse(Long id, String username, String name, String phoneNumber, MemberRole role) {

    public static MemberProfileResponse from(Member member) {
        return new MemberProfileResponse(
            member.getId(),
            member.getUsername(),
            member.getName(),
            PhoneNumbers.format(member.getPhoneNumber()),
            member.getRole()
        );
    }
}
