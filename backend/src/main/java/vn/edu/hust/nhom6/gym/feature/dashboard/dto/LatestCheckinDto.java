package vn.edu.hust.nhom6.gym.feature.dashboard.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinStatus;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Một dòng trong danh sách lượt check-in mới nhất trong ngày trên Dashboard (F6 UC-F6.5).
 * Không trả {@code avatarUrl}; dùng {@code hasAvatar} theo ADR 0011.
 */
public record LatestCheckinDto(
        Long checkinId,
        Long memberId,
        String memberCode,
        String fullName,
        boolean hasAvatar,
        String packageName,
        PackageType packageType,
        CheckinMethod method,
        OffsetDateTime checkedInAt,
        OffsetDateTime checkedOutAt,
        Long durationMinutes,
        CheckinStatus status) {
}
