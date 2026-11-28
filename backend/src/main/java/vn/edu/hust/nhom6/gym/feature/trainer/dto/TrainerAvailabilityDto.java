package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.LocalTime;
import java.time.OffsetDateTime;

/** Khung giờ rảnh lặp hằng tuần của huấn luyện viên (giờ Việt Nam). */
public record TrainerAvailabilityDto(
        Long id,
        Long trainerId,
        Long branchId,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
