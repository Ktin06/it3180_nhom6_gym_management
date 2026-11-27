package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Hội viên đặt buổi PT (UC-F9.2). Không có {@code memberId}: lấy từ token. Không có giờ kết thúc: buổi luôn 60 phút
 * (Q9 = A). Nhận cả tên trường của API nháp trong SRS ({@code ptId}, {@code startTime}).
 *
 * @param trainerId {@code staff.id} của huấn luyện viên
 * @param startAt thời điểm bắt đầu, ISO-8601 có múi giờ, ví dụ {@code 2026-11-23T03:00:00Z} (10:00 giờ Việt Nam)
 */
public record PtSessionBookRequest(
        @JsonAlias("ptId")
        @NotNull(message = "Vui lòng chọn huấn luyện viên.")
        @Positive(message = "Mã huấn luyện viên không hợp lệ.")
        Long trainerId,

        @JsonAlias("startTime")
        @NotNull(message = "Vui lòng chọn giờ bắt đầu.")
        OffsetDateTime startAt) {
}
