package com.luggagestorage.auth.dto;

import com.luggagestorage.member.entity.MemberRole;

public record LoginResponse(String accessToken, String refreshToken, String tokenType, String username, String name, MemberRole role) {

    public static LoginResponse of(String accessToken, String refreshToken, String username, String name, MemberRole role) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", username, name, role);
    }
}
