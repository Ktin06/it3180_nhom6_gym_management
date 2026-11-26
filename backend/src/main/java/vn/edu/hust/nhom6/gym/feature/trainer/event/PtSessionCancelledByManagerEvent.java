package vn.edu.hust.nhom6.gym.feature.trainer.event;

import java.time.OffsetDateTime;

/**
 * Quản lý chi nhánh hủy hộ một buổi PT (UC-F9.7, BR-F9.17). F9 phát trong giao dịch hủy và không gọi F10 trực tiếp.
 * F10 (S2-22.1, loại thông báo {@code PT_SESSION_CANCELLED}) nghe bằng
 * {@code @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)}; ghi bảng thì
 * {@code @Transactional(propagation = REQUIRES_NEW)}. Lỗi ở bên nghe không đảo ngược việc hủy.
 *
 * @param sessionId {@code pt_sessions.id}
 * @param memberId {@code members.id} của hội viên cần báo
 * @param reason lý do quản lý nhập (đã cắt khoảng trắng hai đầu)
 * @param trainerId {@code staff.id} của huấn luyện viên
 * @param branchId chi nhánh của buổi
 * @param startAt giờ bắt đầu buổi đã hủy (UTC)
 * @param cancelledBy {@code users.id} của quản lý
 * @param cancelledAt thời điểm hủy (UTC)
 */
public record PtSessionCancelledByManagerEvent(
        Long sessionId,
        Long memberId,
        String reason,
        Long trainerId,
        Long branchId,
        OffsetDateTime startAt,
        Long cancelledBy,
        OffsetDateTime cancelledAt) {
}
