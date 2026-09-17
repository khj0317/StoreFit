package com.luggagestorage.auth.dto;

public record LoginResponse(String accessToken, String tokenType, String email, String name) {

    public static LoginResponse of(String accessToken, String email, String name) {
        return new LoginResponse(accessToken, "Bearer", email, name);
    }
}
