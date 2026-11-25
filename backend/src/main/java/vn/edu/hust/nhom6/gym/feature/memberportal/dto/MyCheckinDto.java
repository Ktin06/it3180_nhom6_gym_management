package vn.edu.hust.nhom6.gym.feature.memberportal.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Một lượt check-in của chính hội viên đang đăng nhập ({@code GET /api/v1/me/checkins}, UC-F7.2).
 *
 * <p>{@code checkinDate} là ngày Việt Nam đã lưu khi check-in; {@code checkedInAt} và {@code checkedOutAt} (rỗng nếu
 * chưa check-out) là thời điểm UTC.
 */
public record MyCheckinDto(
        Long id,
        Long branchId,
        String branchName,
        Long membershipId,
        String packageName,
        PackageType packageType,
        CheckinMethod method,
        boolean sessionDeducted,
        LocalDate checkinDate,
        OffsetDateTime checkedInAt,
        OffsetDateTime checkedOutAt) { }
