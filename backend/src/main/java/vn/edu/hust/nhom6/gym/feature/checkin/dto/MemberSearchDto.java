package vn.edu.hust.nhom6.gym.feature.checkin.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Kết quả tra cứu tại quầy (UC-F4.1, SRS F4 mục 6.1). Khác API nháp: {@code hasAvatar} thay {@code avatarUrl} (ảnh lấy
 * qua {@code GET /api/v1/members/{id}/avatar}, ADR 0011); {@code memberships} là mọi gói ở mọi trạng thái (UC-F4.1 bước
 * 4), thay tên {@code activeMemberships} của bản nháp.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MemberSearchDto(Long memberId, String memberCode, String fullName, String phone, String gender,
        boolean hasAvatar, MemberStatus status, @JsonProperty("isCurrentlyCheckedIn") boolean currentlyCheckedIn,
        List<HeldPackage> memberships, ClassArrivalDto upcomingClassSession) {

    /** Một gói của hội viên; {@code branchId} rỗng = gói toàn chuỗi; số buổi rỗng với gói TIME. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record HeldPackage(Long id, String packageName, PackageType packageType, Long branchId,
            MembershipStatus status, LocalDate startDate, LocalDate endDate, Integer totalSessions,
            Integer remainingSessions) { }
}
