package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Body tạo hoặc sửa khung giờ rảnh của huấn luyện viên (UC-F9.1). Không nhận {@code trainerId}; mã PT lấy từ JWT.
 * {@code branchId} tùy chọn: bỏ trống thì lấy chi nhánh làm việc của PT ({@code staff.branch_id}).
 */
public record TrainerAvailabilityRequest(
        @Positive(message = "Mã chi nhánh không hợp lệ.")
        Long branchId,
        @NotNull(message = "Vui lòng chọn thứ trong tuần.")
        @Min(value = 1, message = "Thứ phải từ 1 (thứ Hai) đến 7 (Chủ nhật).")
        @Max(value = 7, message = "Thứ phải từ 1 (thứ Hai) đến 7 (Chủ nhật).")
        Integer dayOfWeek,
        @NotNull(message = "Vui lòng nhập giờ bắt đầu.")
        LocalTime startTime,
        @NotNull(message = "Vui lòng nhập giờ kết thúc.")
        LocalTime endTime) {
}
