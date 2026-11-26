package vn.edu.hust.nhom6.gym.feature.membership.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Gói tập hội viên đã mua (bảng {@code memberships}, F3). Map đủ mọi cột theo ERD để các issue sau
 * (kích hoạt S2-09, trừ buổi S2-11/S2-19, bảo lưu, chuyển nhượng, hoàn tiền) không phải sửa entity.
 *
 * <p>Không có setter công khai: trạng thái và số buổi chỉ đổi qua hàm của {@code MembershipService}
 * (G13). Issue sau cần đổi trạng thái thì thêm hàm nghiệp vụ vào lớp này và service, không thêm setter.
 */
@Entity
@Table(name = "memberships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    /** Chép từ {@code packages.type} lúc mua. */
    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 20)
    private PackageType packageType;

    /** Chép từ {@code packages.branch_id}; rỗng = gói dùng toàn chuỗi. */
    @Column(name = "branch_id")
    private Long branchId;

    /** Chi nhánh bán (báo cáo doanh thu). */
    @Column(name = "sold_branch_id", nullable = false)
    private Long soldBranchId;

    @Column(name = "invoice_item_id")
    private Long invoiceItemId;

    /** Dự kiến khi PENDING; S2-09 tính lại lúc hóa đơn PAID (BR-F3-12). */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Ngày cuối còn dùng được: {@code start_date + duration_days - 1}. */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_sessions")
    private Integer totalSessions;

    @Column(name = "remaining_sessions")
    private Integer remainingSessions;

    @Column(name = "freeze_count", nullable = false)
    private Integer freezeCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    @Version
    @Column(nullable = false)
    private Integer version;

    /**
     * Gói PENDING mới, đi kèm một dòng hóa đơn chưa thu tiền (UC-F3.5 bước 6).
     * {@code totalSessions} rỗng với gói TIME; gói SESSION/PT bắt đầu với đủ buổi.
     */
    public static Membership pending(Long memberId, Long packageId, PackageType packageType, Long branchId,
            Long soldBranchId, Long invoiceItemId, LocalDate startDate, LocalDate endDate, Integer totalSessions) {
        Membership m = new Membership();
        m.memberId = memberId;
        m.packageId = packageId;
        m.packageType = packageType;
        m.branchId = branchId;
        m.soldBranchId = soldBranchId;
        m.invoiceItemId = invoiceItemId;
        m.startDate = startDate;
        m.endDate = endDate;
        m.totalSessions = totalSessions;
        m.remainingSessions = totalSessions;
        m.freezeCount = 0;
        m.status = MembershipStatus.PENDING;
        return m;
    }

    /**
     * Hóa đơn PAID, chưa có gói cùng loại đang giữ: PENDING → ACTIVE từ {@code start} (BR-F5-04).
     * Giữ nguyên số ngày đã chốt lúc tạo hóa đơn.
     */
    public void activate(LocalDate start) {
        moveFromPending(MembershipStatus.ACTIVE, start);
    }

    /** Hóa đơn PAID khi đã có gói cùng loại đang giữ: PENDING → UPCOMING từ {@code start} (BR-F3-11). */
    public void scheduleFrom(LocalDate start) {
        moveFromPending(MembershipStatus.UPCOMING, start);
    }

    /** Hóa đơn chờ bị hủy: PENDING → CANCELLED (BR-F5-08). Ngày giữ nguyên để tra cứu. */
    public void cancelPending() {
        requireStatus(MembershipStatus.PENDING);
        this.status = MembershipStatus.CANCELLED;
    }

    private void moveFromPending(MembershipStatus target, LocalDate start) {
        requireStatus(MembershipStatus.PENDING);
        long days = durationDays();
        this.startDate = start;
        this.endDate = start.plusDays(days - 1);
        this.status = target;
    }

    /** Số ngày hiệu lực đã chốt lúc tạo hóa đơn (SRS F3 bước 8: không đọc lại {@code packages.duration_days}). */
    public long durationDays() {
        return endDate.toEpochDay() - startDate.toEpochDay() + 1;
    }

    /**
     * Check-in trừ một buổi (UC-F4.2 bước 5, S2-11.1). Về 0 vẫn giữ ACTIVE đến hết ngày; tác vụ nền đổi EXPIRED.
     *
     * @throws IllegalStateException gói không ACTIVE, không theo buổi, hoặc đã hết buổi (nơi gọi đã kiểm tra trước)
     */
    public void deductOneSession() {
        requireStatus(MembershipStatus.ACTIVE);
        if (remainingSessions == null || remainingSessions <= 0) {
            throw new IllegalStateException("Gói " + getId() + " không còn buổi để trừ");
        }
        remainingSessions = remainingSessions - 1;
    }

    /**
     * Dời gói UPCOMING sớm lên {@code days} ngày, giữ nguyên số ngày hiệu lực (BR-F4.7: gói SESSION trước hết buổi
     * sớm thì gói nối tiếp bắt đầu từ ngày mai).
     */
    public void moveEarlier(long days) {
        requireStatus(MembershipStatus.UPCOMING);
        if (days <= 0) {
            throw new IllegalArgumentException("Số ngày dời phải dương");
        }
        startDate = startDate.minusDays(days);
        endDate = endDate.minusDays(days);
    }

    public void expire() {
        requireStatus(MembershipStatus.ACTIVE);
        status = MembershipStatus.EXPIRED;
    }

    public void activateOnSchedule() {
        requireStatus(MembershipStatus.UPCOMING);
        status = MembershipStatus.ACTIVE;
    }

    public void expireUnstarted() {
        requireStatus(MembershipStatus.UPCOMING);
        status = MembershipStatus.EXPIRED;
    }

    private void requireStatus(MembershipStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Trạng thái gói không hợp lệ: " + status);
        }
    }
}
