package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Tiện ích tạo cookie cho Refresh Token theo quyết định kiến trúc ADR 0008:
 * {@code HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth}.
 */
@Component
public class AuthCookieHelper {

    public static final String REFRESH_COOKIE_NAME = "refresh_token";
    public static final String REFRESH_COOKIE_PATH = "/api/v1/auth";
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final boolean secureCookie;

    public AuthCookieHelper(@Value("${app.auth.cookie-secure:false}") boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public ResponseCookie createRefreshCookie(String rawToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Strict")
            .path(REFRESH_COOKIE_PATH)
            .maxAge(REFRESH_TOKEN_TTL)
            .build();
    }

    public ResponseCookie createCleanRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite("Strict")
            .path(REFRESH_COOKIE_PATH)
            .maxAge(0)
            .build();
    }
}
