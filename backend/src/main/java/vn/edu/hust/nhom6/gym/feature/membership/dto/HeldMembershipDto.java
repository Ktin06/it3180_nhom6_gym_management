package vn.edu.hust.nhom6.gym.feature.membership.dto;

import java.time.LocalDate;

import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Một gói hội viên đang giữ hoặc đang chờ, cho màn hình bán và gia hạn (S2-08.1).
 *
 * @param branchId rỗng = gói toàn chuỗi
 * @param renewalStartDate ngày bắt đầu dự kiến nếu bán thêm gói cùng loại (server tính); rỗng nếu không gia hạn
 *     được hoặc dòng này là gói đang chờ
 */
public record HeldMembershipDto(Long id, Long packageId, String packageName, PackageType packageType,
        Long branchId, MembershipStatus status, LocalDate startDate, LocalDate endDate,
        Integer totalSessions, Integer remainingSessions, LocalDate renewalStartDate) { }
