package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Buổi lớp nhóm được phân công cho huấn luyện viên (đọc theo {@code class_sessions.trainer_id}, G4). */
public record ClassSessionScheduleItem(
        Long id,
        Long classScheduleId,
        Long branchId,
        Long classTypeId,
        String classTypeName,
        Long roomId,
        String roomName,
        LocalDate occurrenceDate,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime originalStartAt,
        Integer capacity,
        String status) {
}
