package com.luggagestorage.auth.dto;

public record LoginResponse(String accessToken, String tokenType, String username, String name) {

    public static LoginResponse of(String accessToken, String username, String name) {
        return new LoginResponse(accessToken, "Bearer", username, name);
    }
}
