package vn.edu.hust.nhom6.gym.feature.checkin.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinStatus;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.membership.entity.MembershipStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Truy vấn <b>chỉ đọc</b> các bảng F4 dùng nhưng không sở hữu (giả định G4): {@code members}, {@code member_avatars},
 * {@code memberships} + {@code packages}, {@code pt_sessions}, {@code class_bookings} + {@code class_sessions} +
 * {@code class_types} + {@code rooms}, {@code membership_freezes}. Mọi thao tác ghi vẫn gọi service của chức năng sở
 * hữu. Mọi câu có tham số. Ngày ({@code date}) đọc dạng chữ {@code YYYY-MM-DD} để không qua quy đổi múi giờ.
 */
@Repository
@RequiredArgsConstructor
public class CheckinLookupRepository {
    private final EntityManager entities;

    /** Id hồ sơ (chưa xóa mềm) theo mã hội viên. */
    public Optional<Long> findMemberIdByCode(String code) {
        return firstLong("SELECT id FROM members WHERE member_code = :v AND deleted_at IS NULL", code);
    }

    /** Id hồ sơ (chưa xóa mềm) theo số điện thoại đã chuẩn hóa. */
    public Optional<Long> findMemberIdByPhone(String phone) {
        return firstLong("SELECT id FROM members WHERE phone = :v AND deleted_at IS NULL", phone);
    }

    public boolean hasAvatar(Long memberId) {
        return !entities.createNativeQuery("SELECT id FROM member_avatars WHERE member_id = :id", Long.class)
            .setParameter("id", memberId).getResultList().isEmpty();
    }

    /** Một gói của hội viên kèm tên gói (màn hình tra cứu, mọi trạng thái). */
    public record MembershipRow(Long id, String packageName, PackageType packageType, Long branchId,
            MembershipStatus status, LocalDate startDate, LocalDate endDate, Integer totalSessions,
            Integer remainingSessions) { }

    /** Mọi gói của hội viên ở mọi trạng thái (UC-F4.1 bước 4), sắp theo ngày bắt đầu mới nhất trước. */
    public List<MembershipRow> findMemberships(Long memberId) {
        return entities.createQuery("""
            SELECT new vn.edu.hust.nhom6.gym.feature.checkin.repository.CheckinLookupRepository$MembershipRow(
                       m.id, p.name, m.packageType, m.branchId, m.status, m.startDate, m.endDate, m.totalSessions,
                       m.remainingSessions)
            FROM Membership m JOIN GymPackage p ON p.id = m.packageId
            WHERE m.memberId = :memberId
            ORDER BY m.startDate DESC, m.id DESC""", MembershipRow.class)
            .setParameter("memberId", memberId).getResultList();
    }

    /**
     * Id gói của các buổi PT {@code BOOKED} của hội viên tại chi nhánh, bắt đầu trong {@code [from, to)} (BR-F4.8),
     * sắp theo giờ bắt đầu.
     */
    public List<Long> findBookedPtMembershipIds(Long memberId, Long branchId, OffsetDateTime from, OffsetDateTime to) {
        List<?> rows = entities.createNativeQuery("""
            SELECT membership_id FROM pt_sessions
            WHERE member_id = :memberId AND branch_id = :branchId AND status = 'BOOKED'
              AND start_at >= :from AND start_at < :to
            ORDER BY start_at, id""", Long.class)
            .setParameter("memberId", memberId).setParameter("branchId", branchId)
            .setParameter("from", from).setParameter("to", to).getResultList();
        return rows.stream().map(Long.class::cast).toList();
    }

    /** Khoảng bảo lưu của một gói FROZEN (E5). */
    public record FreezeRow(Long membershipId, LocalDate fromDate, LocalDate toDate) { }

    /**
     * Gói TIME hoặc SESSION đang FROZEN của hội viên dùng ở chi nhánh {@code branchId} (gói của chi nhánh hoặc toàn
     * chuỗi), kèm đơn bảo lưu APPROVED gần nhất; chưa có đơn thì ngày rỗng.
     */
    public Optional<FreezeRow> findFrozen(Long memberId, Long branchId) {
        List<Object[]> rows = rows(native_("""
            SELECT m.id,
                   to_char(f.from_date, 'YYYY-MM-DD') AS from_date,
                   to_char(f.to_date, 'YYYY-MM-DD') AS to_date
            FROM memberships m
            LEFT JOIN LATERAL (
                SELECT from_date, to_date FROM membership_freezes
                WHERE membership_id = m.id AND status = 'APPROVED'
                ORDER BY from_date DESC, id DESC LIMIT 1) f ON true
            WHERE m.member_id = :memberId AND m.status = 'FROZEN' AND m.package_type IN ('TIME', 'SESSION')
              AND (m.branch_id IS NULL OR m.branch_id = :branchId)
            ORDER BY m.id
            LIMIT 1""")
            .addScalar("id", StandardBasicTypes.LONG).addScalar("from_date", StandardBasicTypes.STRING)
            .addScalar("to_date", StandardBasicTypes.STRING)
            .setParameter("memberId", memberId).setParameter("branchId", branchId));
        return rows.stream().findFirst().map(r -> new FreezeRow((Long) r[0], date(r[1]), date(r[2])));
    }

    /** Một chỗ đặt lớp nhóm kèm tên lớp, phòng, giờ bắt đầu (để lễ tân nhắc hội viên). */
    public record ClassRow(Long bookingId, Long sessionId, String className, String roomName, OffsetDateTime startAt) { }

    /**
     * Lớp gần nhất hội viên đã đặt (BOOKED) ở chi nhánh, bắt đầu trong {@code [from, to)} (UC-F4.1 bước 4: lịch học
     * nhóm trong ngày, buổi sắp bắt đầu).
     */
    public Optional<ClassRow> findUpcomingClass(Long memberId, Long branchId, OffsetDateTime from, OffsetDateTime to) {
        return classRows("""
            WHERE b.member_id = :memberId AND s.branch_id = :branchId AND b.status = 'BOOKED'
              AND s.status <> 'CANCELLED' AND s.start_at >= :from AND s.start_at < :to
            ORDER BY s.start_at, b.id LIMIT 1""", q -> q.setParameter("memberId", memberId)
            .setParameter("branchId", branchId).setParameter("from", from).setParameter("to", to))
            .stream().findFirst();
    }

    /** Thông tin lớp của các chỗ đặt, sắp theo id chỗ. */
    public List<ClassRow> findClassRows(Collection<Long> bookingIds) {
        if (bookingIds.isEmpty()) {
            return List.of();
        }
        return classRows("WHERE b.id IN (:ids) ORDER BY b.id", q -> q.setParameterList("ids", bookingIds));
    }

    private List<ClassRow> classRows(String where, Consumer<NativeQuery<Object[]>> params) {
        NativeQuery<Object[]> query = native_("""
            SELECT b.id, s.id AS session_id, t.name AS class_name, r.name AS room_name, s.start_at
            FROM class_bookings b
            JOIN class_sessions s ON s.id = b.class_session_id
            JOIN class_schedules cs ON cs.id = s.class_schedule_id
            JOIN class_types t ON t.id = cs.class_type_id
            JOIN rooms r ON r.id = s.room_id
            """ + where)
            .addScalar("id", StandardBasicTypes.LONG).addScalar("session_id", StandardBasicTypes.LONG)
            .addScalar("class_name", StandardBasicTypes.STRING).addScalar("room_name", StandardBasicTypes.STRING)
            .addScalar("start_at", StandardBasicTypes.OFFSET_DATE_TIME);
        params.accept(query);
        return rows(query).stream().map(r -> new ClassRow((Long) r[0], (Long) r[1], (String) r[2], (String) r[3],
            (OffsetDateTime) r[4])).toList();
    }

    private Optional<Long> firstLong(String sql, String value) {
        List<?> rows = entities.createNativeQuery(sql, Long.class).setParameter("v", value).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of((Long) rows.getFirst());
    }

    private static LocalDate date(Object text) {
        return text == null ? null : LocalDate.parse((String) text);
    }

    @SuppressWarnings("unchecked")
    private NativeQuery<Object[]> native_(String sql) {
        return entities.createNativeQuery(sql).unwrap(NativeQuery.class);
    }

    private static List<Object[]> rows(NativeQuery<Object[]> query) {
        return query.getResultList();
    }

    // ---------------------------------------------------------------------------------------------
    // S2-11.2: lịch sử check-in trong ngày (UC-F4.5, G4)
    // ---------------------------------------------------------------------------------------------

    /** Một dòng lịch sử check-in (chỉ đọc nhiều bảng: checkins, members, member_avatars, memberships, packages, staff). */
    public record HistoryRow(Long checkinId, Long memberId, String memberCode, String fullName, boolean hasAvatar,
            Long membershipId, String packageName, PackageType packageType, CheckinMethod method,
            boolean sessionDeducted, LocalDate checkinDate, OffsetDateTime checkedInAt, OffsetDateTime checkedOutAt,
            Long createdBy, String createdByName) { }

    /**
     * Lịch sử check-in tại chi nhánh trong khoảng nửa mở {@code [from, to)} trên {@code checked_in_at} để dùng chỉ mục
     * {@code ix_checkins_branch_checked_in (branch_id, checked_in_at)}, mới nhất trước, có phân trang.
     */
    public Page<HistoryRow> findHistory(Long branchId, OffsetDateTime from, OffsetDateTime to, CheckinStatus status,
            CheckinMethod method, String keywordPattern, int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE c.branch_id = :branchId"
            + " AND c.checked_in_at >= :from AND c.checked_in_at < :to");
        if (status == CheckinStatus.CHECKED_IN) {
            where.append(" AND c.checked_out_at IS NULL");
        } else if (status == CheckinStatus.CHECKED_OUT) {
            where.append(" AND c.checked_out_at IS NOT NULL");
        }
        if (method != null) {
            where.append(" AND c.method = :method");
        }
        if (keywordPattern != null) {
            where.append(" AND (LOWER(m.full_name) LIKE :keyword ESCAPE '!'"
                + " OR LOWER(m.member_code) LIKE :keyword ESCAPE '!')");
        }

        String whereClause = where.toString();
        var countQuery = entities.createNativeQuery(
            "SELECT count(*) FROM checkins c JOIN members m ON m.id = c.member_id" + whereClause);
        bindHistoryParams(countQuery, branchId, from, to, method, keywordPattern);
        long total = ((Number) countQuery.getSingleResult()).longValue();
        PageRequest pageable = PageRequest.of(page, size);
        if (total == 0 || (long) page * size >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        NativeQuery<Object[]> dataQuery = native_("""
            SELECT c.id AS checkin_id,
                   c.member_id,
                   m.member_code,
                   m.full_name,
                   EXISTS (SELECT 1 FROM member_avatars ma WHERE ma.member_id = c.member_id) AS has_avatar,
                   c.membership_id,
                   p.name AS package_name,
                   ms.package_type,
                   c.method,
                   c.session_deducted,
                   to_char(c.checkin_date, 'YYYY-MM-DD') AS checkin_date,
                   c.checked_in_at,
                   c.checked_out_at,
                   c.created_by,
                   st.full_name AS created_by_name
            FROM checkins c
            JOIN members m ON m.id = c.member_id
            JOIN memberships ms ON ms.id = c.membership_id
            JOIN packages p ON p.id = ms.package_id
            LEFT JOIN staff st ON st.user_id = c.created_by
            """ + whereClause + " ORDER BY c.checked_in_at DESC, c.id DESC LIMIT :limit OFFSET :offset")
            .addScalar("checkin_id", StandardBasicTypes.LONG)
            .addScalar("member_id", StandardBasicTypes.LONG)
            .addScalar("member_code", StandardBasicTypes.STRING)
            .addScalar("full_name", StandardBasicTypes.STRING)
            .addScalar("has_avatar", StandardBasicTypes.BOOLEAN)
            .addScalar("membership_id", StandardBasicTypes.LONG)
            .addScalar("package_name", StandardBasicTypes.STRING)
            .addScalar("package_type", StandardBasicTypes.STRING)
            .addScalar("method", StandardBasicTypes.STRING)
            .addScalar("session_deducted", StandardBasicTypes.BOOLEAN)
            .addScalar("checkin_date", StandardBasicTypes.STRING)
            .addScalar("checked_in_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("checked_out_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .addScalar("created_by", StandardBasicTypes.LONG)
            .addScalar("created_by_name", StandardBasicTypes.STRING);
        bindHistoryParams(dataQuery, branchId, from, to, method, keywordPattern);
        dataQuery.setParameter("limit", size).setParameter("offset", page * size);

        List<HistoryRow> items = rows(dataQuery).stream().map(r -> new HistoryRow(
            (Long) r[0], (Long) r[1], (String) r[2], (String) r[3], Boolean.TRUE.equals(r[4]),
            (Long) r[5], (String) r[6], PackageType.valueOf((String) r[7]), CheckinMethod.valueOf((String) r[8]),
            Boolean.TRUE.equals(r[9]), date(r[10]), utc((OffsetDateTime) r[11]), utc((OffsetDateTime) r[12]),
            (Long) r[13], (String) r[14])).toList();
        return new PageImpl<>(items, pageable, total);
    }

    private static void bindHistoryParams(jakarta.persistence.Query query, Long branchId, OffsetDateTime from,
            OffsetDateTime to, CheckinMethod method, String keywordPattern) {
        query.setParameter("branchId", branchId).setParameter("from", from).setParameter("to", to);
        if (method != null) {
            query.setParameter("method", method.name());
        }
        if (keywordPattern != null) {
            query.setParameter("keyword", keywordPattern);
        }
    }

    private static OffsetDateTime utc(OffsetDateTime ts) {
        return ts == null ? null : ts.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
