package com.luggagestorage.member.dto;

import com.luggagestorage.member.entity.Member;

public record SignupResponse(Long id, String username, String name, String email) {

    public static SignupResponse from(Member member) {
        return new SignupResponse(member.getId(), member.getUsername(), member.getName(), member.getEmail());
    }
}
