package vn.edu.hust.nhom6.gym.feature.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import vn.edu.hust.nhom6.gym.feature.auth.dto.AuthMeResponse;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/** API xác thực và thông tin phiên đăng nhập hiện tại (F1). */
@Tag(name = "F1 – Tài khoản và phân quyền")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Operation(summary = "Lấy thông tin tài khoản hiện tại từ JWT")
    @GetMapping("/me")
    public AuthMeResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        UserRole role = UserRole.valueOf(jwt.getClaimAsString("role"));
        return new AuthMeResponse(userId, email, role);
    }
}
