package com.luggagestorage.auth.dto;

import com.luggagestorage.member.entity.MemberRole;

/** @param demo 체험 계정이면 true (화면에 체험 중 안내를 띄운다) */
public record LoginResponse(String accessToken, String refreshToken, String tokenType, String username, String name, MemberRole role,
                            boolean demo) {

    public static LoginResponse of(String accessToken, String refreshToken, String username, String name, MemberRole role,
                                   boolean demo) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", username, name, role, demo);
    }
}
