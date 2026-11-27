package vn.edu.hust.nhom6.gym.feature.memberportal.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceItemType;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Truy vấn <b>chỉ đọc</b> tổng hợp nhiều bảng cho cổng hội viên (F7 UC-F7.1–UC-F7.3, giả định G4):
 * {@code memberships + packages + branches}, {@code checkins + branches + memberships + packages},
 * {@code invoices + invoice_items + branches}. Mọi câu truy vấn có tham số đặt tên, lọc chặt theo {@code memberId}
 * lấy từ token ở service, không sửa service hay repository của F3, F4, F5.
 */
@Repository
@RequiredArgsConstructor
public class MemberPortalQueryRepository {
    private final EntityManager entities;

    /** Bản ghi gói tập đọc từ {@code memberships + packages + branches}. */
    public record MembershipRow(
            Long id,
            Long packageId,
            String packageName,
            PackageType packageType,
            Long branchId,
            String branchName,
            MembershipStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Integer totalSessions,
            Integer remainingSessions) { }

    /** Bản ghi lịch sử check-in đọc từ {@code checkins + branches + memberships + packages}. */
    public record CheckinRow(
            Long id,
            Long branchId,
            String branchName,
            Long membershipId,
            String packageName,
            PackageType packageType,
            CheckinMethod method,
            boolean sessionDeducted,
            LocalDate checkinDate,
            OffsetDateTime checkedInAt,
            OffsetDateTime checkedOutAt) { }

    /** Bản ghi hóa đơn đọc từ {@code invoices + branches}. */
    public record InvoiceRow(
            Long id,
            String code,
            Long branchId,
            String branchName,
            InvoiceSource source,
            int subtotal,
            int discount,
            int total,
            InvoiceStatus status,
            OffsetDateTime paidAt,
            OffsetDateTime createdAt) { }

    /** Bản ghi dòng hóa đơn đọc theo lô từ {@code invoice_items}. */
    public record InvoiceItemRow(
            Long id,
            Long invoiceId,
            InvoiceItemType itemType,
            Long packageId,
            String description,
            int quantity,
            int unitPrice,
            int amount,
            int discountAmount) { }

    /**
     * Toàn bộ gói tập của hội viên ở mọi trạng thái (UC-F7.1), sắp theo ngày bắt đầu mới nhất trước.
     * Ngày đọc dạng chữ {@code YYYY-MM-DD} để không qua quy đổi múi giờ.
     */
    public List<MembershipRow> findMemberships(Long memberId) {
        NativeQuery<Object[]> query = native_("""
            SELECT m.id,
                   m.package_id,
                   p.name AS package_name,
                   m.package_type,
                   m.branch_id,
                   b.name AS branch_name,
                   m.status,
                   to_char(m.start_date, 'YYYY-MM-DD') AS start_date,
                   to_char(m.end_date, 'YYYY-MM-DD') AS end_date,
                   m.total_sessions,
                   m.remaining_sessions
            FROM memberships m
            JOIN packages p ON p.id = m.package_id
            LEFT JOIN branches b ON b.id = m.branch_id
            WHERE m.member_id = :memberId
            ORDER BY m.start_date DESC, m.id DESC""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("package_id", StandardBasicTypes.LONG)
            .addScalar("package_name", StandardBasicTypes.STRING)
            .addScalar("package_type", StandardBasicTypes.STRING)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("branch_name", StandardBasicTypes.STRING)
            .addScalar("status", StandardBasicTypes.STRING)
            .addScalar("start_date", StandardBasicTypes.STRING)
            .addScalar("end_date", StandardBasicTypes.STRING)
            .addScalar("total_sessions", StandardBasicTypes.INTEGER)
            .addScalar("remaining_sessions", StandardBasicTypes.INTEGER);
        query.setParameter("memberId", memberId);

        return rows(query).stream()
            .map(r -> new MembershipRow(
                (Long) r[0],
                (Long) r[1],
                (String) r[2],
                PackageType.valueOf((String) r[3]),
                (Long) r[4],
                (String) r[5],
                MembershipStatus.valueOf((String) r[6]),
                LocalDate.parse((String) r[7]),
                LocalDate.parse((String) r[8]),
                (Integer) r[9],
                (Integer) r[10]))
            .toList();
    }

    /**
     * Lịch sử check-in phân trang của hội viên, mới nhất trước (UC-F7.2).
     * Dùng chỉ mục {@code (member_id, checked_in_at)} trên bảng {@code checkins}.
     */
    public Page<CheckinRow> findCheckins(Long memberId, int page, int size) {
        long total = ((Number) entities.createNativeQuery("SELECT count(*) FROM checkins WHERE member_id = :memberId")
            .setParameter("memberId", memberId)
            .getSingleResult()).longValue();
        PageRequest pageable = PageRequest.of(page, size);
        if (total == 0 || (long) page * size >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        NativeQuery<Object[]> query = native_("""
            SELECT c.id,
                   c.branch_id,
                   b.name AS branch_name,
                   c.membership_id,
                   p.name AS package_name,
                   ms.package_type,
                   c.method,
                   c.session_deducted,
                   to_char(c.checkin_date, 'YYYY-MM-DD') AS checkin_date,
                   c.checked_in_at,
                   c.checked_out_at
            FROM checkins c
            JOIN branches b ON b.id = c.branch_id
            JOIN memberships ms ON ms.id = c.membership_id
            JOIN packages p ON p.id = ms.package_id
            WHERE c.member_id = :memberId
            ORDER BY c.checked_in_at DESC, c.id DESC
            LIMIT :limit OFFSET :offset""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("branch_name", StandardBasicTypes.STRING)
            .addScalar("membership_id", StandardBasicTypes.LONG)
            .addScalar("package_name", StandardBasicTypes.STRING)
            .addScalar("package_type", StandardBasicTypes.STRING)
            .addScalar("method", StandardBasicTypes.STRING)
            .addScalar("session_deducted", StandardBasicTypes.BOOLEAN)
            .addScalar("checkin_date", StandardBasicTypes.STRING)
            .addScalar("checked_in_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("checked_out_at", StandardBasicTypes.OFFSET_DATE_TIME);
        query.setParameter("memberId", memberId)
            .setParameter("limit", size)
            .setParameter("offset", page * size);

        List<CheckinRow> content = rows(query).stream()
            .map(r -> new CheckinRow(
                (Long) r[0],
                (Long) r[1],
                (String) r[2],
                (Long) r[3],
                (String) r[4],
                PackageType.valueOf((String) r[5]),
                CheckinMethod.valueOf((String) r[6]),
                Boolean.TRUE.equals(r[7]),
                LocalDate.parse((String) r[8]),
                utc((OffsetDateTime) r[9]),
                utc((OffsetDateTime) r[10])))
            .toList();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Danh sách hóa đơn phân trang của hội viên, mới nhất trước (UC-F7.3).
     * Dùng chỉ mục {@code (member_id, created_at)} trên bảng {@code invoices}; hóa đơn khách lẻ
     * ({@code member_id IS NULL}) không bao giờ khớp {@code i.member_id = :memberId}.
     */
    public Page<InvoiceRow> findInvoices(Long memberId, int page, int size) {
        long total = ((Number) entities.createNativeQuery("SELECT count(*) FROM invoices WHERE member_id = :memberId")
            .setParameter("memberId", memberId)
            .getSingleResult()).longValue();
        PageRequest pageable = PageRequest.of(page, size);
        if (total == 0 || (long) page * size >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        NativeQuery<Object[]> query = native_("""
            SELECT i.id,
                   i.code,
                   i.branch_id,
                   b.name AS branch_name,
                   i.source,
                   i.subtotal,
                   i.discount,
                   i.total,
                   i.status,
                   i.paid_at,
                   i.created_at
            FROM invoices i
            JOIN branches b ON b.id = i.branch_id
            WHERE i.member_id = :memberId
            ORDER BY i.created_at DESC, i.id DESC
            LIMIT :limit OFFSET :offset""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("code", StandardBasicTypes.STRING)
            .addScalar("branch_id", StandardBasicTypes.LONG)
            .addScalar("branch_name", StandardBasicTypes.STRING)
            .addScalar("source", StandardBasicTypes.STRING)
            .addScalar("subtotal", StandardBasicTypes.INTEGER)
            .addScalar("discount", StandardBasicTypes.INTEGER)
            .addScalar("total", StandardBasicTypes.INTEGER)
            .addScalar("status", StandardBasicTypes.STRING)
            .addScalar("paid_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("created_at", StandardBasicTypes.OFFSET_DATE_TIME);
        query.setParameter("memberId", memberId)
            .setParameter("limit", size)
            .setParameter("offset", page * size);

        List<InvoiceRow> content = rows(query).stream()
            .map(r -> new InvoiceRow(
                (Long) r[0],
                (String) r[1],
                (Long) r[2],
                (String) r[3],
                InvoiceSource.valueOf((String) r[4]),
                (Integer) r[5],
                (Integer) r[6],
                (Integer) r[7],
                InvoiceStatus.valueOf((String) r[8]),
                utc((OffsetDateTime) r[9]),
                utc((OffsetDateTime) r[10])))
            .toList();
        return new PageImpl<>(content, pageable, total);
    }

    /** Đọc theo lô mọi dòng {@code invoice_items} của các hóa đơn trên trang hiện tại (tránh truy vấn N+1). */
    public Map<Long, List<InvoiceItemRow>> findInvoiceItems(Collection<Long> invoiceIds) {
        if (invoiceIds.isEmpty()) {
            return Map.of();
        }
        NativeQuery<Object[]> query = native_("""
            SELECT id,
                   invoice_id,
                   item_type,
                   package_id,
                   description,
                   quantity,
                   unit_price,
                   amount,
                   discount_amount
            FROM invoice_items
            WHERE invoice_id IN (:invoiceIds)
            ORDER BY invoice_id, id""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("invoice_id", StandardBasicTypes.LONG)
            .addScalar("item_type", StandardBasicTypes.STRING)
            .addScalar("package_id", StandardBasicTypes.LONG)
            .addScalar("description", StandardBasicTypes.STRING)
            .addScalar("quantity", StandardBasicTypes.INTEGER)
            .addScalar("unit_price", StandardBasicTypes.INTEGER)
            .addScalar("amount", StandardBasicTypes.INTEGER)
            .addScalar("discount_amount", StandardBasicTypes.INTEGER);
        query.setParameterList("invoiceIds", invoiceIds);

        Map<Long, List<InvoiceItemRow>> byInvoice = new LinkedHashMap<>();
        for (Object[] r : rows(query)) {
            InvoiceItemRow item = new InvoiceItemRow(
                (Long) r[0],
                (Long) r[1],
                InvoiceItemType.valueOf((String) r[2]),
                (Long) r[3],
                (String) r[4],
                (Integer) r[5],
                (Integer) r[6],
                (Integer) r[7],
                (Integer) r[8]);
            byInvoice.computeIfAbsent(item.invoiceId(), ignored -> new ArrayList<>()).add(item);
        }
        return byInvoice;
    }

    @SuppressWarnings("unchecked")
    private NativeQuery<Object[]> native_(String sql) {
        return entities.createNativeQuery(sql).unwrap(NativeQuery.class);
    }

    private static List<Object[]> rows(NativeQuery<Object[]> query) {
        return query.getResultList();
    }

    private static OffsetDateTime utc(OffsetDateTime ts) {
        return ts == null ? null : ts.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
