package vn.edu.hust.nhom6.gym.feature.checkin.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Một dòng trong lịch sử check-in tại quầy ({@code GET /api/v1/checkins}, UC-F4.5 bước 4). Không có {@code avatarUrl}:
 * giao diện kiểm tra {@code hasAvatar} rồi tải ảnh qua {@code GET /api/v1/members/{memberId}/avatar} (ADR 0011).
 * {@code checkedOutAt} và {@code durationMinutes} rỗng ({@code null}) khi {@code status = CHECKED_IN}.
 */
public record CheckinHistoryItemDto(Long checkinId, Long memberId, String memberCode, String fullName,
        boolean hasAvatar, Long membershipId, String packageName, PackageType packageType, CheckinMethod method,
        boolean sessionDeducted, LocalDate checkinDate, OffsetDateTime checkedInAt, OffsetDateTime checkedOutAt,
        Long durationMinutes, CheckinStatus status, Long createdBy, String createdByName) { }
