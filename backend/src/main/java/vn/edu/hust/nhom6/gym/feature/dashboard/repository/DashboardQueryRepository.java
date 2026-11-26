package vn.edu.hust.nhom6.gym.feature.dashboard.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Truy vấn <b>chỉ đọc</b> tổng hợp nhiều bảng cho Dashboard cơ bản (F6 UC-F6.1–UC-F6.5, giả định G4):
 * {@code checkins}, {@code payments + invoices + members}, {@code memberships + members + member_avatars + packages}.
 * Mọi câu truy vấn dùng tham số đặt tên, không gọi repository của F3, F4, F5.
 */
@Repository
@RequiredArgsConstructor
public class DashboardQueryRepository {
    private static final String EXPIRING_FROM_WHERE = """
            FROM memberships ms
            JOIN members m ON m.id = ms.member_id
            JOIN packages p ON p.id = ms.package_id
            WHERE ms.status = 'ACTIVE'
              AND m.deleted_at IS NULL
              AND (
                  (ms.end_date >= CAST(:today AS date) AND ms.end_date < CAST(:expireBefore AS date))
                  OR (ms.package_type = 'SESSION' AND ms.remaining_sessions <= :sessionThreshold)
              )
              AND (ms.branch_id = :branchId OR (ms.branch_id IS NULL AND m.home_branch_id = :branchId))
              AND NOT EXISTS (
                  SELECT 1
                  FROM memberships up
                  WHERE up.member_id = ms.member_id
                    AND up.package_type = ms.package_type
                    AND up.branch_id IS NOT DISTINCT FROM ms.branch_id
                    AND up.status = 'UPCOMING'
              )
            """;

    private final EntityManager entities;

    /** Tổng số hội viên khác nhau đang tập (chưa check-out, VD-25 GĐ-1) và tổng lượt check-in trong ngày. */
    public record CheckinStatsRow(long currentlyInGym, long todayCheckinCount) { }

    /** Tổng doanh thu ngày tại quầy (VND, tách tiền mặt và chuyển khoản). */
    public record RevenueRow(Integer totalRevenue, Integer cashRevenue, Integer transferRevenue) { }

    /** Bản ghi thô của một gói tập sắp hết hạn / sắp hết buổi. */
    public record ExpiringMembershipRow(
            Long membershipId,
            Long memberId,
            String memberCode,
            String fullName,
            String phone,
            boolean hasAvatar,
            String packageName,
            PackageType packageType,
            LocalDate endDate,
            Integer remainingSessions) { }

    /** Bản ghi thô của một lượt check-in mới nhất trong ngày. */
    public record LatestCheckinRow(
            Long checkinId,
            Long memberId,
            String memberCode,
            String fullName,
            boolean hasAvatar,
            String packageName,
            PackageType packageType,
            CheckinMethod method,
            OffsetDateTime checkedInAt,
            OffsetDateTime checkedOutAt) { }

    /**
     * Đếm số hội viên khác nhau đang tập ({@code checked_out_at IS NULL}, VD-25 GĐ-1) và tổng lượt check-in trong
     * khoảng nửa mở {@code [dayStart, dayEnd)} (UC-F6.1, UC-F6.2). Dùng chỉ mục {@code (branch_id, checked_in_at)}.
     */
    public CheckinStatsRow countCheckinStats(Long branchId, OffsetDateTime dayStart, OffsetDateTime dayEnd) {
        NativeQuery<Object[]> query = native_("""
            SELECT count(DISTINCT member_id) FILTER (WHERE checked_out_at IS NULL) AS currently_in_gym,
                   count(*) AS today_checkin_count
            FROM checkins
            WHERE branch_id = :branchId
              AND checked_in_at >= :dayStart AND checked_in_at < :dayEnd""")
            .addScalar("currently_in_gym", StandardBasicTypes.LONG)
            .addScalar("today_checkin_count", StandardBasicTypes.LONG);
        query.setParameter("branchId", branchId)
            .setParameter("dayStart", dayStart)
            .setParameter("dayEnd", dayEnd);
        Object[] row = query.getSingleResult();
        return new CheckinStatsRow(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }

    /**
     * Tổng hợp số lượt check-in theo từng giờ Việt Nam trong khoảng nửa mở {@code [dayStart, dayEnd)} (UC-F6.2).
     * Trả bản đồ {@code giờ (0..23) -> số lượt}.
     */
    public Map<Integer, Long> findHourlyCheckinCounts(Long branchId, OffsetDateTime dayStart, OffsetDateTime dayEnd) {
        NativeQuery<Object[]> query = native_("""
            SELECT CAST(extract(hour FROM (checked_in_at AT TIME ZONE 'Asia/Ho_Chi_Minh')) AS integer) AS hour_bucket,
                   count(*) AS checkin_count
            FROM checkins
            WHERE branch_id = :branchId
              AND checked_in_at >= :dayStart AND checked_in_at < :dayEnd
            GROUP BY hour_bucket
            ORDER BY hour_bucket""")
            .addScalar("hour_bucket", StandardBasicTypes.INTEGER)
            .addScalar("checkin_count", StandardBasicTypes.LONG);
        query.setParameter("branchId", branchId)
            .setParameter("dayStart", dayStart)
            .setParameter("dayEnd", dayEnd);

        Map<Integer, Long> result = new LinkedHashMap<>();
        for (Object[] r : rows(query)) {
            result.put((Integer) r[0], ((Number) r[1]).longValue());
        }
        return result;
    }

    /**
     * Tổng doanh thu hôm nay tại chi nhánh (UC-F6.3, BR-F6.2, BR-F6.3): chỉ tính {@code payments.status = 'CONFIRMED'}
     * thuộc {@code invoices.status = 'PAID'} trong {@code [dayStart, dayEnd)}. Bắt buộc {@code LEFT JOIN members}
     * để không bỏ sót hóa đơn khách lẻ ({@code member_id IS NULL}).
     */
    public RevenueRow sumTodayRevenue(Long branchId, OffsetDateTime dayStart, OffsetDateTime dayEnd) {
        NativeQuery<Object[]> query = native_("""
            SELECT coalesce(sum(p.amount), 0) AS total_revenue,
                   coalesce(sum(CASE WHEN p.method = 'CASH' THEN p.amount ELSE 0 END), 0) AS cash_revenue,
                   coalesce(sum(CASE WHEN p.method = 'BANK_TRANSFER' THEN p.amount ELSE 0 END), 0) AS transfer_revenue
            FROM payments p
            JOIN invoices i ON p.invoice_id = i.id
            LEFT JOIN members m ON i.member_id = m.id
            WHERE i.branch_id = :branchId
              AND i.status = 'PAID'
              AND p.status = 'CONFIRMED'
              AND p.paid_at >= :dayStart AND p.paid_at < :dayEnd""")
            .addScalar("total_revenue", StandardBasicTypes.LONG)
            .addScalar("cash_revenue", StandardBasicTypes.LONG)
            .addScalar("transfer_revenue", StandardBasicTypes.LONG);
        query.setParameter("branchId", branchId)
            .setParameter("dayStart", dayStart)
            .setParameter("dayEnd", dayEnd);
        Object[] row = query.getSingleResult();
        return new RevenueRow(
            Math.toIntExact(((Number) row[0]).longValue()),
            Math.toIntExact(((Number) row[1]).longValue()),
            Math.toIntExact(((Number) row[2]).longValue()));
    }

    /** Đếm tổng số gói thỏa điều kiện cảnh báo tại chi nhánh (UC-F6.4, BR-F6.5). */
    public long countExpiringMemberships(Long branchId, LocalDate today, LocalDate expireBefore, int sessionThreshold) {
        Number count = (Number) entities.createNativeQuery("SELECT count(*) " + EXPIRING_FROM_WHERE)
            .setParameter("branchId", branchId)
            .setParameter("today", today.toString())
            .setParameter("expireBefore", expireBefore.toString())
            .setParameter("sessionThreshold", sessionThreshold)
            .getSingleResult();
        return count.longValue();
    }

    /**
     * Danh sách gói tập sắp hết hạn / sắp hết buổi (UC-F6.4, BR-F6.5): gói {@code SESSION} đã về 0 buổi xếp lên đầu,
     * còn lại theo {@code end_date ASC, id ASC}. Ngày đọc dạng chữ {@code YYYY-MM-DD} để không qua quy đổi múi giờ.
     */
    public List<ExpiringMembershipRow> findExpiringMemberships(Long branchId, LocalDate today, LocalDate expireBefore,
            int sessionThreshold, int limit) {
        NativeQuery<Object[]> query = native_("""
            SELECT ms.id AS membership_id,
                   m.id AS member_id,
                   m.member_code,
                   m.full_name,
                   m.phone,
                   EXISTS (SELECT 1 FROM member_avatars ma WHERE ma.member_id = m.id) AS has_avatar,
                   p.name AS package_name,
                   ms.package_type,
                   to_char(ms.end_date, 'YYYY-MM-DD') AS end_date,
                   ms.remaining_sessions
            """ + EXPIRING_FROM_WHERE + """
            ORDER BY CASE WHEN ms.package_type = 'SESSION' AND ms.remaining_sessions = 0 THEN 0 ELSE 1 END ASC,
                     ms.end_date ASC,
                     ms.id ASC
            LIMIT :limit""")
            .addScalar("membership_id", StandardBasicTypes.LONG)
            .addScalar("member_id", StandardBasicTypes.LONG)
            .addScalar("member_code", StandardBasicTypes.STRING)
            .addScalar("full_name", StandardBasicTypes.STRING)
            .addScalar("phone", StandardBasicTypes.STRING)
            .addScalar("has_avatar", StandardBasicTypes.BOOLEAN)
            .addScalar("package_name", StandardBasicTypes.STRING)
            .addScalar("package_type", StandardBasicTypes.STRING)
            .addScalar("end_date", StandardBasicTypes.STRING)
            .addScalar("remaining_sessions", StandardBasicTypes.INTEGER);
        query.setParameter("branchId", branchId)
            .setParameter("today", today.toString())
            .setParameter("expireBefore", expireBefore.toString())
            .setParameter("sessionThreshold", sessionThreshold)
            .setParameter("limit", limit);

        return rows(query).stream()
            .map(r -> new ExpiringMembershipRow(
                (Long) r[0],
                (Long) r[1],
                (String) r[2],
                (String) r[3],
                (String) r[4],
                Boolean.TRUE.equals(r[5]),
                (String) r[6],
                PackageType.valueOf((String) r[7]),
                LocalDate.parse((String) r[8]),
                (Integer) r[9]))
            .toList();
    }

    /**
     * Danh sách lượt check-in mới nhất trong ngày tại chi nhánh (UC-F6.5), mới nhất trước. Dùng {@code hasAvatar}
     * thay cho {@code avatar_url} (ADR 0011).
     */
    public List<LatestCheckinRow> findLatestCheckins(Long branchId, OffsetDateTime dayStart, OffsetDateTime dayEnd,
            int limit) {
        NativeQuery<Object[]> query = native_("""
            SELECT c.id AS checkin_id,
                   c.member_id,
                   m.member_code,
                   m.full_name,
                   EXISTS (SELECT 1 FROM member_avatars ma WHERE ma.member_id = c.member_id) AS has_avatar,
                   p.name AS package_name,
                   ms.package_type,
                   c.method,
                   c.checked_in_at,
                   c.checked_out_at
            FROM checkins c
            JOIN members m ON m.id = c.member_id
            JOIN memberships ms ON ms.id = c.membership_id
            JOIN packages p ON p.id = ms.package_id
            WHERE c.branch_id = :branchId
              AND c.checked_in_at >= :dayStart AND c.checked_in_at < :dayEnd
            ORDER BY c.checked_in_at DESC, c.id DESC
            LIMIT :limit""")
            .addScalar("checkin_id", StandardBasicTypes.LONG)
            .addScalar("member_id", StandardBasicTypes.LONG)
            .addScalar("member_code", StandardBasicTypes.STRING)
            .addScalar("full_name", StandardBasicTypes.STRING)
            .addScalar("has_avatar", StandardBasicTypes.BOOLEAN)
            .addScalar("package_name", StandardBasicTypes.STRING)
            .addScalar("package_type", StandardBasicTypes.STRING)
            .addScalar("method", StandardBasicTypes.STRING)
            .addScalar("checked_in_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("checked_out_at", StandardBasicTypes.OFFSET_DATE_TIME);
        query.setParameter("branchId", branchId)
            .setParameter("dayStart", dayStart)
            .setParameter("dayEnd", dayEnd)
            .setParameter("limit", limit);

        return rows(query).stream()
            .map(r -> new LatestCheckinRow(
                (Long) r[0],
                (Long) r[1],
                (String) r[2],
                (String) r[3],
                Boolean.TRUE.equals(r[4]),
                (String) r[5],
                PackageType.valueOf((String) r[6]),
                CheckinMethod.valueOf((String) r[7]),
                utc((OffsetDateTime) r[8]),
                utc((OffsetDateTime) r[9])))
            .toList();
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
