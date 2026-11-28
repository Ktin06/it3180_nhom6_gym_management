package vn.edu.hust.nhom6.gym.feature.trainer.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;

/**
 * Một buổi PT một kèm một (bảng {@code pt_sessions}, F9). Không có setter: trạng thái chỉ đổi qua hàm nghiệp vụ, để luôn
 * thỏa CHECK {@code end_at > start_at} và {@code (status = DONE) = (commission_amount có giá trị)}.
 *
 * <p>S2-19.2: {@link #book} (giữ chỗ, không trừ buổi; BR-F9.5) và {@link #cancel} (UC-F9.7). S2-19.3 thêm xác nhận
 * ({@code DONE}, {@code confirmed_at}, {@code commission_amount}) và ghi vắng ({@code NO_SHOW}) vào lớp này.
 * Không có cột {@code time} (VD-13 không áp dụng).
 */
@Entity
@Table(name = "pt_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PtSession extends BaseEntity {

    /** Độ dài cố định một buổi PT (Q9 = A). */
    public static final int DURATION_MINUTES = 60;

    /** {@code staff.id} của huấn luyện viên. */
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** Gói PT giữ chỗ cho buổi; cùng hội viên với {@link #memberId} (kiểm tra trong service). */
    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    /** Chi nhánh diễn ra buổi = chi nhánh làm việc của huấn luyện viên. */
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PtSessionStatus status;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "commission_amount")
    private Integer commissionAmount;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    /** {@code users.id} của tài khoản bấm đặt. */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(columnDefinition = "text")
    private String note;

    /** Buổi mới {@code BOOKED} dài {@link #DURATION_MINUTES} phút. Không trừ buổi của gói (BR-F9.5). */
    public static PtSession book(Long trainerId, Long memberId, Long membershipId, Long branchId,
            OffsetDateTime startAt, Long createdBy) {
        PtSession session = new PtSession();
        session.trainerId = trainerId;
        session.memberId = memberId;
        session.membershipId = membershipId;
        session.branchId = branchId;
        session.startAt = startAt;
        session.endAt = endOf(startAt);
        session.status = PtSessionStatus.BOOKED;
        session.createdBy = createdBy;
        return session;
    }

    /** Giờ kết thúc của buổi bắt đầu lúc {@code startAt}. */
    public static OffsetDateTime endOf(OffsetDateTime startAt) {
        return startAt.plusMinutes(DURATION_MINUTES);
    }

    /**
     * Hủy buổi đang {@code BOOKED} (UC-F9.7). Không đổi gói: buổi chưa bị trừ nên không cần hoàn.
     *
     * @throws BusinessRuleException (422, E3) khi buổi không còn {@code BOOKED}
     */
    public void cancel(OffsetDateTime at) {
        requireBooked();
        status = PtSessionStatus.CANCELLED;
        cancelledAt = at;
    }

    /** @throws BusinessRuleException (422, E3) khi buổi không còn {@code BOOKED} */
    public void requireBooked() {
        if (status != PtSessionStatus.BOOKED) {
            throw new BusinessRuleException("Buổi tập không còn ở trạng thái đã đặt.");
        }
    }

    // S2-19.3: xác nhận, ghi vắng, chuyển gói (UC-F9.3)

    /** Buổi đã diễn ra: {@code DONE}, chốt hoa hồng một lần (BR-F9.8). */
    public void confirm(OffsetDateTime at, int commission) {
        requireBooked();
        status = PtSessionStatus.DONE;
        confirmedAt = at;
        commissionAmount = commission;
    }

    /**
     * Hội viên vắng không báo: {@code NO_SHOW}, không hoa hồng (BR-F9.18). {@code confirmed_at} ghi lúc PT (hoặc quản
     * lý) ghi nhận kết quả buổi.
     */
    public void markNoShow(OffsetDateTime at) {
        requireBooked();
        status = PtSessionStatus.NO_SHOW;
        confirmedAt = at;
    }

    /** Buổi tương lai chuyển sang gói PT nối tiếp khi gói cũ hết buổi (VD-21 GĐ-8 = A). */
    public void moveToMembership(Long newMembershipId) {
        requireBooked();
        membershipId = newMembershipId;
    }
}
