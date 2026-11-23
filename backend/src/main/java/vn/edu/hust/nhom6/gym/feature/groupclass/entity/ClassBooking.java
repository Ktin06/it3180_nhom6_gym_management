package vn.edu.hust.nhom6.gym.feature.groupclass.entity;

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
 * Một chỗ đặt lớp nhóm (bảng {@code class_bookings}). Không có setter: trạng thái chỉ đổi qua hàm nghiệp vụ, để luôn
 * thỏa CHECK {@code (status = CANCELLED) = (cancelled_at, cancel_reason khác rỗng)} và
 * {@code is_late_cancel = false OR status = CANCELLED}.
 *
 * <p>Mỗi hội viên có tối đa một chỗ chưa hủy trong một buổi (chỉ mục {@code uq_active_booking}); hủy rồi thì đặt lại
 * bằng một dòng mới, dòng cũ giữ làm lịch sử (AC-F8.1.1-06). Điểm danh (S2-17.2): {@link #markAttended},
 * {@link #markNoShow} chỉ từ BOOKED; {@link #correctAttendance} chỉ từ ATTENDED hoặc NO_SHOW (BR-F8.6-05).
 */
@Entity
@Table(name = "class_bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassBooking extends BaseEntity {

    @Column(name = "class_session_id", nullable = false)
    private Long classSessionId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** {@code users.id} của tài khoản bấm đặt: hội viên, hoặc lễ tân khi đặt hộ (BR-F8-01). */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassBookingStatus status;

    @Column(name = "booked_at")
    private OffsetDateTime bookedAt;

    /** Check-in tại quầy (F4) ghi qua {@link #markArrived}; điểm danh của F8 không đổi cột này. */
    @Column(name = "arrived_at")
    private OffsetDateTime arrivedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason", length = 30)
    private CancelReason cancelReason;

    @Column(name = "is_late_cancel", nullable = false)
    private boolean lateCancel;

    @Column(name = "attendance_marked_by")
    private Long attendanceMarkedBy;

    /** Chỗ mới BOOKED. */
    public static ClassBooking book(Long classSessionId, Long memberId, Long createdBy, OffsetDateTime at) {
        ClassBooking booking = new ClassBooking();
        booking.classSessionId = classSessionId;
        booking.memberId = memberId;
        booking.createdBy = createdBy;
        booking.status = ClassBookingStatus.BOOKED;
        booking.bookedAt = at;
        return booking;
    }

    /**
     * Hủy chỗ đang BOOKED. {@code late} chỉ được {@code true} khi hội viên tự hủy hoặc lễ tân hủy hộ sát giờ; hủy do
     * chi nhánh hủy buổi luôn {@code false} (BR-F8.7-05).
     *
     * @throws BusinessRuleException (422) khi chỗ không còn BOOKED
     */
    public void cancel(OffsetDateTime at, CancelReason reason, boolean late) {
        if (status != ClassBookingStatus.BOOKED) {
            throw new BusinessRuleException("Chỗ đặt không còn ở trạng thái đã đặt, không hủy được.");
        }
        status = ClassBookingStatus.CANCELLED;
        cancelledAt = at;
        cancelReason = reason;
        lateCancel = late && reason != CancelReason.SESSION_CANCELLED;
    }

    /**
     * Điểm danh lần đầu: có mặt (UC-F8.6, BR-F8.6-05). Không đổi {@code arrived_at} của check-in (BR-F8.6-02).
     *
     * @param by {@code users.id} người ghi (BR-F8.6-01)
     * @throws BusinessRuleException (422) khi chỗ không còn BOOKED
     */
    public void markAttended(Long by) {
        markFirst(ClassBookingStatus.ATTENDED, by);
    }

    /**
     * Điểm danh lần đầu: vắng mặt. Nơi gọi lưu, flush rồi mới tính vi phạm (BR-F8.6-03).
     *
     * @throws BusinessRuleException (422) khi chỗ không còn BOOKED
     */
    public void markNoShow(Long by) {
        markFirst(ClassBookingStatus.NO_SHOW, by);
    }

    /**
     * Sửa kết quả đã ghi (ATTENDED ↔ NO_SHOW). Không tính vi phạm, không đổi khóa (BR-F8.6-03).
     *
     * @return trạng thái trước khi sửa, để ghi {@code audit_logs}
     * @throws BusinessRuleException (422) khi chỗ chưa có kết quả, đã hủy, hoặc kết quả mới trùng kết quả đang có
     */
    public ClassBookingStatus correctAttendance(ClassBookingStatus result, Long by) {
        if (status != ClassBookingStatus.ATTENDED && status != ClassBookingStatus.NO_SHOW) {
            throw new BusinessRuleException(status == ClassBookingStatus.CANCELLED
                ? "Chỗ đặt đã hủy, không sửa điểm danh được."
                : "Chỗ đặt chưa được điểm danh; hãy điểm danh lần đầu thay vì sửa.");
        }
        if (result != ClassBookingStatus.ATTENDED && result != ClassBookingStatus.NO_SHOW) {
            throw new IllegalArgumentException("Kết quả điểm danh chỉ là ATTENDED hoặc NO_SHOW.");
        }
        if (result == status) {
            throw new BusinessRuleException("Kết quả điểm danh mới trùng kết quả đang có.");
        }
        ClassBookingStatus old = status;
        status = result;
        attendanceMarkedBy = by;
        return old;
    }

    /**
     * Check-in tại quầy (F4, UC-F4.3) ghi lúc hội viên đến. Chỉ ghi cho chỗ BOOKED chưa có {@code arrived_at}; không đổi
     * {@code status} (điểm danh vẫn do huấn luyện viên, BR-F8.6-02). Chỗ đã NO_SHOW, ATTENDED, CANCELLED giữ nguyên, để
     * {@code updated_at} của NO_SHOW còn là mốc đếm vi phạm (BR-F8.3-02).
     *
     * @return {@code true} nếu đã ghi; {@code false} nếu chỗ không còn BOOKED hoặc đã có {@code arrived_at}
     */
    public boolean markArrived(OffsetDateTime at) {
        if (status != ClassBookingStatus.BOOKED || arrivedAt != null) {
            return false;
        }
        arrivedAt = at;
        return true;
    }

    private void markFirst(ClassBookingStatus result, Long by) {
        if (status != ClassBookingStatus.BOOKED) {
            throw new BusinessRuleException(status == ClassBookingStatus.CANCELLED
                ? "Chỗ đặt đã hủy, không điểm danh được."
                : "Chỗ đặt đã được điểm danh; hãy dùng chức năng sửa điểm danh.");
        }
        status = result;
        attendanceMarkedBy = by;
    }
}
