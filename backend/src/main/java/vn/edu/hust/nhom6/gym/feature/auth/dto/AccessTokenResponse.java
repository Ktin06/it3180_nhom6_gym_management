package vn.edu.hust.nhom6.gym.feature.auth.dto;

/** Phản hồi chứa JWT Access Token trả về từ {@code POST /api/v1/auth/register}, {@code /login} và {@code /refresh}. */
public record AccessTokenResponse(
    String accessToken
) {
}
