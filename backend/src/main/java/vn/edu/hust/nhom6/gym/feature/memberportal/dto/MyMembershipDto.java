package vn.edu.hust.nhom6.gym.feature.memberportal.dto;

import java.time.LocalDate;

import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Một gói tập của chính hội viên đang đăng nhập ({@code GET /api/v1/me/memberships}, UC-F7.1).
 *
 * <p>API trả mã trạng thái ({@code status}); nhãn tiếng Việt theo BR-F7.2 do giao diện hiển thị.
 * {@code branchId} và {@code branchName} rỗng ({@code null}) nghĩa là gói dùng toàn chuỗi.
 * {@code totalSessions} và {@code remainingSessions} rỗng với gói {@code TIME}.
 */
public record MyMembershipDto(
        Long id,
        Long packageId,
        String packageName,
        PackageType packageType,
        Long branchId,
        String branchName,
        MembershipStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Integer totalSessions,
        Integer remainingSessions) { }
