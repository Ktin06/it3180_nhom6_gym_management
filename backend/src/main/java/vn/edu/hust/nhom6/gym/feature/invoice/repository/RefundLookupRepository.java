package vn.edu.hust.nhom6.gym.feature.invoice.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Truy vấn <b>chỉ đọc</b> để kiểm tra "gói chưa dùng" trước khi hoàn tiền (BR-F5-14, giả định G4): đọc
 * {@code invoice_items}, {@code memberships}, {@code checkins}, {@code pt_sessions}, {@code class_bookings} +
 * {@code class_sessions}. Không gọi service F4, F8, F9; mọi thao tác ghi vẫn qua service của chức năng sở hữu.
 */
@Repository
@RequiredArgsConstructor
public class RefundLookupRepository {
    private final EntityManager entities;

    /**
     * Một gói sinh ra từ hóa đơn, kèm các dấu hiệu "đã dùng".
     *
     * @param invoiceItemId dòng hóa đơn PACKAGE sinh ra gói
     * @param hasCheckin có lượt check-in gắn với gói (điều kiện 2), kể cả check-in bằng gói PT
     * @param hasPtSession có buổi PT gắn với gói (điều kiện 3), mọi trạng thái kể cả CANCELLED
     * @param hasUpcomingClassBooking gói TIME và hội viên còn chỗ lớp BOOKED của buổi chưa kết thúc (điều kiện 4,
     *     "chưa diễn ra" hiểu là chưa xong, nên buổi đang học cũng chặn); luôn {@code false} với gói SESSION, PT
     */
    public record UsageRow(Long invoiceItemId, Long membershipId, PackageType packageType, MembershipStatus status,
            boolean hasCheckin, boolean hasPtSession, boolean hasUpcomingClassBooking) {

        /** Điều kiện 1–3 của BR-F5-14: gói không còn ACTIVE/UPCOMING, hoặc đã check-in, hoặc đã có buổi PT. */
        public boolean used() {
            return (status != MembershipStatus.ACTIVE && status != MembershipStatus.UPCOMING)
                || hasCheckin || hasPtSession;
        }
    }

    /** Mọi gói của các dòng PACKAGE trong hóa đơn, sắp theo id dòng. Đọc sau khi đã khóa dòng hội viên. */
    public List<UsageRow> findUsage(Long invoiceId, OffsetDateTime now) {
        List<?> rows = entities.createNativeQuery("""
            SELECT ii.id, m.id, m.package_type, m.status,
                   EXISTS (SELECT 1 FROM checkins c WHERE c.membership_id = m.id),
                   EXISTS (SELECT 1 FROM pt_sessions p WHERE p.membership_id = m.id),
                   m.package_type = 'TIME' AND EXISTS (
                       SELECT 1 FROM class_bookings cb
                       JOIN class_sessions cs ON cs.id = cb.class_session_id
                       WHERE cb.member_id = m.member_id AND cb.status = 'BOOKED' AND cs.end_at > :now)
            FROM invoice_items ii
            JOIN memberships m ON m.invoice_item_id = ii.id
            WHERE ii.invoice_id = :invoiceId AND ii.item_type = 'PACKAGE'
            ORDER BY ii.id""")
            .setParameter("invoiceId", invoiceId)
            .setParameter("now", now)
            .getResultList();
        return rows.stream().map(Object[].class::cast).map(r -> new UsageRow(
            ((Number) r[0]).longValue(), ((Number) r[1]).longValue(),
            PackageType.valueOf((String) r[2]), MembershipStatus.valueOf((String) r[3]),
            (Boolean) r[4], (Boolean) r[5], (Boolean) r[6])).toList();
    }
}
