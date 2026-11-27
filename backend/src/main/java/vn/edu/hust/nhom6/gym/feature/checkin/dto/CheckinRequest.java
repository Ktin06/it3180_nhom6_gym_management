package vn.edu.hust.nhom6.gym.feature.checkin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Body của {@code POST /api/v1/checkins}: chỉ có hội viên; gói do server chọn (UC-F4.2 bước 5). */
public record CheckinRequest(
    @NotNull(message = "Vui lòng chọn hội viên cần check-in.")
    @Positive(message = "ID hội viên phải lớn hơn 0.")
    Long memberId) { }
