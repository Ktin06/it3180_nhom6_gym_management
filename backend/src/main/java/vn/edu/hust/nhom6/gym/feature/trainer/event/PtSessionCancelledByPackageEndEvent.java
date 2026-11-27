package vn.edu.hust.nhom6.gym.feature.trainer.event;

import java.time.OffsetDateTime;

/**
 * Buổi PT tương lai bị hệ thống hủy vì gói PT của buổi vừa hết buổi mà không có gói nối tiếp đủ buổi, đủ hạn
 * (S2-19.3, VD-21 GĐ-8 = A). Phát trong giao dịch xác nhận hoặc ghi vắng; F10 (S2-22.1) nghe
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} để báo hội viên. F9 không gọi F10 trực tiếp.
 *
 * @param sessionId buổi bị hủy
 * @param memberId hội viên cần báo
 * @param trainerId {@code staff.id} của huấn luyện viên
 * @param branchId chi nhánh của buổi
 * @param startAt giờ bắt đầu buổi (UTC)
 * @param membershipId gói PT vừa hết buổi
 * @param cancelledAt thời điểm hủy (UTC)
 */
public record PtSessionCancelledByPackageEndEvent(Long sessionId, Long memberId, Long trainerId, Long branchId,
        OffsetDateTime startAt, Long membershipId, OffsetDateTime cancelledAt) {
}
