package vn.edu.hust.nhom6.gym.feature.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.config.LogoutTokenDecoder;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AccessTokenResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AuthMeResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.ChangePasswordRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.LoginRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthCookieHelper;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;

/** API xác thực và phiên đăng nhập người dùng (F1). */
@Tag(name = "F1 – Tài khoản và phân quyền")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieHelper cookieHelper;
    private final LogoutTokenDecoder logoutTokenDecoder;

    public AuthController(AuthService authService, AuthCookieHelper cookieHelper,
            LogoutTokenDecoder logoutTokenDecoder) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
        this.logoutTokenDecoder = logoutTokenDecoder;
    }

    @Operation(summary = "Đăng ký tài khoản hội viên")
    @PostMapping("/register")
    public ResponseEntity<AccessTokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        ResponseCookie cookie = cookieHelper.createRefreshCookie(result.rawRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new AccessTokenResponse(result.accessToken()));
    }

    @Operation(summary = "Đăng nhập tài khoản bằng email")
    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        ResponseCookie cookie = cookieHelper.createRefreshCookie(result.rawRefreshToken());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new AccessTokenResponse(result.accessToken()));
    }

    @Operation(summary = "Làm mới phiên đăng nhập (xoay vòng refresh token)")
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = AuthCookieHelper.REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        try {
            AuthService.AuthResult result = authService.refresh(refreshToken);
            ResponseCookie cookie = cookieHelper.createRefreshCookie(result.rawRefreshToken());
            return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AccessTokenResponse(result.accessToken()));
        } catch (UnauthorizedException ex) {
            ResponseCookie cleanCookie = cookieHelper.createCleanRefreshCookie();
            response.addHeader(HttpHeaders.SET_COOKIE, cleanCookie.toString());
            throw ex;
        }
    }

    @Operation(
        summary = "Đăng xuất tài khoản",
        description = "Nếu gửi kèm Authorization Bearer JWT có chữ ký hợp lệ (kể cả đã hết hạn), chỉ thu hồi và "
            + "xóa cookie refresh_token khi token trong cookie thuộc cùng người dùng với JWT, để tránh làm mất "
            + "phiên của tab khác. JWT sai chữ ký thì coi như không gửi kèm."
    )
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieHelper.REFRESH_COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        Long authenticatedUserId = logoutTokenDecoder.userIdOf(authorization).orElse(null);
        boolean shouldClearCookie;
        if (authenticatedUserId != null) {
            shouldClearCookie = authService.logoutForUser(refreshToken, authenticatedUserId);
        } else {
            authService.logout(refreshToken);
            shouldClearCookie = true;
        }

        if (shouldClearCookie) {
            return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieHelper.createCleanRefreshCookie().toString())
                .build();
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lấy thông tin tài khoản hiện tại từ JWT")
    @GetMapping("/me")
    public AuthMeResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return authService.getCurrentUser(jwt);
    }

    @Operation(summary = "Đổi mật khẩu và thu hồi toàn bộ phiên đăng nhập")
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.from(auth), request);
        return ResponseEntity.noContent().build();
    }
}
