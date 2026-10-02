package com.luggagestorage.verification;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 요청한 사용자의 IP. 배포에서는 Vercel → Render를 거쳐 오므로 서버가 직접 보는 주소는 프록시 주소이고,
 * 원래 주소는 X-Forwarded-For의 맨 앞에 있다.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first.length() > 64 ? first.substring(0, 64) : first;
            }
        }
        return request.getRemoteAddr();
    }
}
