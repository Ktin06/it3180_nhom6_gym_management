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

    private void moveFromPending(MembershipStatus target, LocalDate start) {
        requireStatus(MembershipStatus.PENDING);
        long days = durationDays();
        this.startDate = start;
        this.endDate = start.plusDays(days - 1);
        this.status = target;
    }

    private void requireStatus(MembershipStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Gói " + getId() + " đang " + status + ", cần " + expected);
        }
    }

    /** Số ngày hiệu lực đã chốt lúc tạo hóa đơn (SRS F3 bước 8: không đọc lại {@code packages.duration_days}). */
    public long durationDays() {
        return endDate.toEpochDay() - startDate.toEpochDay() + 1;
    }

    /**
     * Hoàn tiền hóa đơn bán nhầm (UC-F5.5, S2-38.1): ACTIVE hoặc UPCOMING → CANCELLED. Ngày, số buổi giữ nguyên để tra
     * cứu. Nơi gọi đã kiểm tra gói chưa dùng (BR-F5-14) và trả 422; ở đây chỉ chặn lỗi lập trình.
     */
    public void cancelForRefund() {
        if (status != MembershipStatus.ACTIVE && status != MembershipStatus.UPCOMING) {
            throw new IllegalStateException("Gói " + getId() + " đang " + status + ", không hoàn tiền được");
        }
        this.status = MembershipStatus.CANCELLED;
    }

    // ---------------------------------------------------------------------------------------------
    // S2-19.3: trừ buổi PT khi xác nhận hoặc ghi vắng (UC-F9.3, BR-F9.7, BR-F9.18)
    // ---------------------------------------------------------------------------------------------

    /**
     * Gói PT vừa bị trừ buổi cuối: ACTIVE → EXPIRED ngay (BR-F9.7). Khác gói SESSION (vẫn ACTIVE tới hết ngày).
     *
     * @throws IllegalStateException gói không ACTIVE hoặc còn buổi
     */
    public void expireAfterLastPtSession() {
        requireStatus(MembershipStatus.ACTIVE);
        if (!Integer.valueOf(0).equals(remainingSessions)) {
            throw new IllegalStateException("Gói " + getId() + " còn buổi, không hết hạn được");
        }
        status = MembershipStatus.EXPIRED;
    }

    /**
     * Gói UPCOMING nối tiếp được kéo về bắt đầu từ {@code start} và ACTIVE ngay, giữ nguyên số ngày hiệu lực
     * ({@code end_date = start_date + số ngày - 1}; BR-F9.7).
     */
    public void activateEarly(LocalDate start) {
        requireStatus(MembershipStatus.UPCOMING);
        long days = durationDays();
        startDate = start;
        endDate = start.plusDays(days - 1);
        status = MembershipStatus.ACTIVE;
    }

    /**
     * Trừ một buổi trên gói đã EXPIRED vì hết ngày, cho buổi PT đã diễn ra khi gói còn hạn mà quản lý xác nhận muộn
     * (trưởng nhóm chốt 20/11). Không đổi trạng thái.
     *
     * @throws IllegalStateException gói không EXPIRED hoặc không còn buổi
     */
    public void deductSessionAfterExpiry() {
        requireStatus(MembershipStatus.EXPIRED);
        if (remainingSessions == null || remainingSessions <= 0) {
            throw new IllegalStateException("Gói " + getId() + " không còn buổi để trừ");
        }
        remainingSessions = remainingSessions - 1;
    }

    // ---------------------------------------------------------------------------------------------
    // S2-07.2: tác vụ nền 00:00 (AC-F3.5.1-10, BR-F4.7)
    // ---------------------------------------------------------------------------------------------

    /** Tác vụ nền: gói hết hạn (hoặc gói SESSION 0 buổi hôm nay chưa trừ) ACTIVE → EXPIRED. Ngày, số buổi giữ nguyên. */
    public void expire() {
        requireStatus(MembershipStatus.ACTIVE);
        status = MembershipStatus.EXPIRED;
    }

    /** Tác vụ nền: gói nối tiếp tới ngày bắt đầu UPCOMING → ACTIVE, giữ nguyên ngày đã xếp. */
    public void activateOnSchedule() {
        requireStatus(MembershipStatus.UPCOMING);
        status = MembershipStatus.ACTIVE;
    }

    /**
     * Tác vụ nền chạy bù: gói UPCOMING mà cả hạn đã trôi qua khi máy chủ tắt ({@code end_date} trước hôm nay)
     * → EXPIRED thẳng, không qua ACTIVE (giả định prototype S2-07.2).
     */
    public void expireUnstarted() {
        requireStatus(MembershipStatus.UPCOMING);
        status = MembershipStatus.EXPIRED;
    }
}
