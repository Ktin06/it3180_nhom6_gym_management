package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.OffsetDateTime;
import java.util.List;

import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSession;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSessionStatus;

/**
 * Kết quả xác nhận hoặc ghi vắng buổi PT (UC-F9.3). Thời điểm đều UTC.
 *
 * @param commissionAmount hoa hồng đã chốt (VND); {@code null} khi {@code NO_SHOW}
 * @param remainingSessions số buổi còn lại của gói vừa bị trừ
 * @param membershipStatus trạng thái gói vừa bị trừ ({@code EXPIRED} nếu đây là buổi cuối)
 * @param activatedMembershipId gói PT nối tiếp vừa được kéo về ACTIVE hôm nay; {@code null} nếu không có
 * @param movedSessionIds buổi tương lai đã chuyển sang gói nối tiếp (GĐ-8)
 * @param cancelledSessionIds buổi tương lai bị hủy vì không còn gói đủ buổi, đủ hạn (GĐ-8)
 */
public record PtSessionCompletionDto(
        Long id,
        Long trainerId,
        Long memberId,
        Long membershipId,
        Long branchId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        PtSessionStatus status,
        OffsetDateTime confirmedAt,
        Integer commissionAmount,
        Integer remainingSessions,
        MembershipStatus membershipStatus,
        Long activatedMembershipId,
        List<Long> movedSessionIds,
        List<Long> cancelledSessionIds) {

    public static PtSessionCompletionDto of(PtSession s, Integer remainingSessions, MembershipStatus membershipStatus,
            Long activatedMembershipId, List<Long> moved, List<Long> cancelled) {
        return new PtSessionCompletionDto(s.getId(), s.getTrainerId(), s.getMemberId(), s.getMembershipId(),
            s.getBranchId(), s.getStartAt(), s.getEndAt(), s.getStatus(), s.getConfirmedAt(), s.getCommissionAmount(),
            remainingSessions, membershipStatus, activatedMembershipId, List.copyOf(moved), List.copyOf(cancelled));
    }
}
