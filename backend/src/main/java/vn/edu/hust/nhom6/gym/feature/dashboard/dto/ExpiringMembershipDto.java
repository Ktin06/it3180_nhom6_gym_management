package vn.edu.hust.nhom6.gym.feature.dashboard.dto;

import java.time.LocalDate;

import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Một dòng cảnh báo gói tập sắp hết hạn hoặc sắp hết buổi trên Dashboard (F6 UC-F6.4).
 * Không trả {@code avatarUrl}; dùng {@code hasAvatar} theo ADR 0011.
 */
public record ExpiringMembershipDto(
        Long membershipId,
        Long memberId,
        String memberCode,
        String fullName,
        String phone,
        boolean hasAvatar,
        String packageName,
        PackageType packageType,
        LocalDate endDate,
        long daysRemaining,
        Integer remainingSessions,
        ExpiringWarningReason warningReason) {
}
