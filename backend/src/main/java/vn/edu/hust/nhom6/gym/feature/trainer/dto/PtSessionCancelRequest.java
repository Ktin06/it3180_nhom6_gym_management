package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import jakarta.validation.constraints.Size;

/**
 * Hủy buổi PT (UC-F9.7). {@code reason} bắt buộc khi quản lý chi nhánh hủy hộ (kiểm ở service vì phụ thuộc vai trò);
 * hội viên, lễ tân có thể bỏ trống cả body.
 */
public record PtSessionCancelRequest(
        @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự.")
        String reason) {
}
