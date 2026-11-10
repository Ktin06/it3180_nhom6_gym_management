package vn.edu.hust.nhom6.gym.feature.auth.dto;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/** Thông tin tài khoản đang đăng nhập trả về từ {@code GET /api/v1/auth/me}. */
public record AuthMeResponse(
    Long userId,
    String email,
    UserRole role
) {
}
