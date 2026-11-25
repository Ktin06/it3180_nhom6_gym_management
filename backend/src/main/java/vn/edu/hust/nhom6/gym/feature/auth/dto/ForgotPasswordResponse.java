package vn.edu.hust.nhom6.gym.feature.auth.dto;

/** Phản hồi câu thông báo chung cho {@code POST /api/v1/auth/forgot-password} (UC-F1.5, Q7 C1). */
public record ForgotPasswordResponse(String message) {
}
