package vn.edu.hust.nhom6.gym.feature.checkin.dto;

import java.time.OffsetDateTime;

/** Kết quả check-out thành công ({@code POST /api/v1/checkins/{id}/checkout}, UC-F4.4, SRS F4 mục 6.3). */
public record CheckoutResultDto(Long checkinId, OffsetDateTime checkedOutAt, long durationMinutes, String message) {
    public static final String SUCCESS = "Check-out thành công. Hẹn gặp lại quý hội viên!";
}
