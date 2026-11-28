package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.LocalDate;
import java.util.List;

/** Lịch dạy tổng hợp của huấn luyện viên trong khoảng ngày Việt Nam {@code [from, to]} (UC-F9.1). */
public record TrainerScheduleResponse(
        Long trainerId,
        Long branchId,
        LocalDate from,
        LocalDate to,
        List<TrainerAvailabilityDto> availabilities,
        List<PtSessionScheduleItem> ptSessions,
        List<ClassSessionScheduleItem> classSessions) {
}
