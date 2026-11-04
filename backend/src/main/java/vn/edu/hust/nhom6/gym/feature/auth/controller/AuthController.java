package vn.edu.hust.nhom6.gym.feature.auth.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AccessTokenResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AuthMeResponse;
import vn.edu.hust.nhom6.gym.feature.auth.dto.LoginRequest;
import vn.edu.hust.nhom6.gym.feature.auth.dto.RegisterRequest;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthCookieHelper;
import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;

/** API xác thực và phiên đăng nhập người dùng (F1). */
@Tag(name = "F1 – Tài khoản và phân quyền")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieHelper cookieHelper;

    public AuthController(AuthService authService, AuthCookieHelper cookieHelper) {
        this.authService = authService;
        this.cookieHelper = cookieHelper;
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
            @CookieValue(name = AuthCookieHelper.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        AuthService.AuthResult result = authService.refresh(refreshToken);
        ResponseCookie cookie = cookieHelper.createRefreshCookie(result.rawRefreshToken());
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(new AccessTokenResponse(result.accessToken()));
    }

    @Operation(summary = "Đăng xuất tài khoản")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookieHelper.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie cleanCookie = cookieHelper.createCleanRefreshCookie();
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
            .build();
    }

    @Operation(summary = "Lấy thông tin tài khoản hiện tại từ JWT")
    @GetMapping("/me")
    public AuthMeResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
        return new AuthMeResponse(userId, email, role);
    }
}
