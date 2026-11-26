package vn.edu.hust.nhom6.gym.feature.checkin.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * Một lượt vào phòng tập (bảng {@code checkins}, F4). Không có setter: lượt mới tạo bằng {@link #manual}; check-out
 * (S2-11.2) thêm hàm nghiệp vụ vào lớp này.
 *
 * <p>{@code checkin_date} là ngày Việt Nam do ứng dụng tính (BR-F4.3), không dùng {@code CURRENT_DATE} của PostgreSQL.
 * Chỉ mục {@code uq_one_deduction_per_day} bảo đảm mỗi gói chỉ một lượt {@code session_deducted = true} mỗi ngày.
 */
@Entity
@Table(name = "checkins")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checkin extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /** {@code users.id} của lễ tân hoặc quản lý bấm check-in. */
    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckinMethod method;

    /** Điểm khớp khuôn mặt (F20, v5); luôn rỗng với MANUAL. */
    @Column(name = "match_score", precision = 5, scale = 4)
    private BigDecimal matchScore;

    @Column(name = "checkin_date", nullable = false)
    private LocalDate checkinDate;

    @Column(name = "session_deducted", nullable = false)
    private boolean sessionDeducted;

    @Column(name = "checked_in_at", nullable = false)
    private OffsetDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private OffsetDateTime checkedOutAt;

    /** Lượt check-in thủ công tại quầy (UC-F4.2 bước 6). */
    public static Checkin manual(Long memberId, Long membershipId, Long branchId, Long createdBy, LocalDate checkinDate,
            boolean sessionDeducted, OffsetDateTime checkedInAt) {
        Checkin c = new Checkin();
        c.memberId = memberId;
        c.membershipId = membershipId;
        c.branchId = branchId;
        c.createdBy = createdBy;
        c.method = CheckinMethod.MANUAL;
        c.checkinDate = checkinDate;
        c.sessionDeducted = sessionDeducted;
        c.checkedInAt = checkedInAt;
        return c;
    }

    /** Ghi nhận giờ ra về cho lượt tập đang mở (UC-F4.4 bước 4); entity không có setter chung. */
    public void checkOut(OffsetDateTime at) {
        if (at == null) {
            throw new IllegalArgumentException("Thời điểm check-out không được rỗng.");
        }
        if (this.checkedOutAt != null) {
            throw new IllegalStateException("Lượt check-in đã được check-out.");
        }
        if (at.isBefore(this.checkedInAt)) {
            throw new IllegalArgumentException("Thời điểm check-out không được trước thời điểm check-in.");
        }
        this.checkedOutAt = at;
    }
}
