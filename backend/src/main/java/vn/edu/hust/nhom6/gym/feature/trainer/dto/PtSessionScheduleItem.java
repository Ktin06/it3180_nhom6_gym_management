package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.OffsetDateTime;

/** Buổi PT hiển thị trong lịch dạy của huấn luyện viên (đọc từ {@code pt_sessions}, G4). */
public record PtSessionScheduleItem(
        Long id,
        Long memberId,
        String memberCode,
        String memberName,
        Long membershipId,
        Long branchId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String status,
        String note) {
}
