package com.luggagestorage.member.dto;

import com.luggagestorage.member.entity.Member;

public record SignupResponse(Long id, String email, String name) {

    public static SignupResponse from(Member member) {
        return new SignupResponse(member.getId(), member.getEmail(), member.getName());
    }
}
