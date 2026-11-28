package vn.edu.hust.nhom6.gym.feature.trainer.dto;

import java.time.OffsetDateTime;

import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSession;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSessionStatus;

/** Một buổi PT trả về sau khi đặt hoặc hủy. Thời điểm đều UTC. */
public record PtSessionDto(
        Long id,
        Long trainerId,
        Long memberId,
        Long membershipId,
        Long branchId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        PtSessionStatus status,
        OffsetDateTime cancelledAt,
        OffsetDateTime createdAt) {

    public static PtSessionDto of(PtSession s) {
        return new PtSessionDto(s.getId(), s.getTrainerId(), s.getMemberId(), s.getMembershipId(), s.getBranchId(),
            s.getStartAt(), s.getEndAt(), s.getStatus(), s.getCancelledAt(), s.getCreatedAt());
    }
}
