package vn.edu.hust.nhom6.gym.feature.checkin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;

/**
 * Dữ liệu dựng bằng SQL cho kiểm thử check-in (S2-11.1). Dựa trên {@link BookingFixtures}: tài khoản, hội viên, gói,
 * buổi lớp, chỗ đặt. {@link #reset()} còn:
 * <ul>
 *   <li>ghi giờ mở cửa CN01, CN02 bằng SQL {@code '06:00'} – {@code '22:00'} (VD-13: đọc lại phải ra đúng giờ đó);</li>
 *   <li>ghi lại tham số F8 trong {@code system_configs} (qua {@code BookingFixtures.reset()}, VD-11).</li>
 * </ul>
 */
final class CheckinFixtures {
    private final JdbcTemplate jdbc;
    final BookingFixtures b;
    Long cn01;
    Long cn02;
    Long receptionist;
    Long receptionistCn02;
    Long manager;
    Long owner;
    Long trainerUser;
    private Long ptTrainer;
    private int seq;

    CheckinFixtures(JdbcTemplate jdbc, JwtTokenService tokens) {
        this.jdbc = jdbc;
        this.b = new BookingFixtures(jdbc, tokens);
    }

    void reset() {
        b.reset();
        cn01 = b.cn01;
        cn02 = b.cn02;
        seq = 0;
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS member_avatars (
                id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                member_id bigint NOT NULL UNIQUE REFERENCES members(id),
                content bytea NOT NULL,
                content_type varchar(50) NOT NULL DEFAULT 'image/jpeg',
                size_bytes integer NOT NULL,
                uploaded_by bigint NOT NULL REFERENCES users(id),
                created_at timestamptz NOT NULL DEFAULT now(),
                updated_at timestamptz NOT NULL DEFAULT now()
            )""");
        jdbc.execute("ALTER TABLE member_avatars ADD COLUMN IF NOT EXISTS id bigint GENERATED ALWAYS AS IDENTITY");
        jdbc.execute("TRUNCATE TABLE member_avatars CASCADE");
        jdbc.update("UPDATE branches SET open_time = '06:00', close_time = '22:00' WHERE id IN (?, ?)", cn01, cn02);
        receptionist = b.receptionist;
        receptionistCn02 = b.receptionistCn02;
        manager = b.f.user("quan-ly@example.com", UserRole.BRANCH_MANAGER, cn01);
        owner = b.f.user("chu-chuoi@example.com", UserRole.SUPER_ADMIN, null);
        trainerUser = b.f.user("hlv-pt@example.com", UserRole.TRAINER, cn01);
        ptTrainer = b.f.staffId(trainerUser);
    }

    /** Hội viên CN01 (có tài khoản), chưa có gói. */
    Account member() {
        return b.account(cn01);
    }

    Long time(Long memberId) {
        return membership(memberId, "TIME", cn01, "ACTIVE", null);
    }

    /** Gói SESSION ACTIVE CN01 còn {@code remaining} buổi, hiệu lực 01/10 – 31/12/2026. */
    Long session(Long memberId, int remaining) {
        return membership(memberId, "SESSION", cn01, "ACTIVE", remaining);
    }

    Long pt(Long memberId) {
        return membership(memberId, "PT", cn01, "ACTIVE", 10);
    }

    /** Gói hiệu lực 01/10 – 31/12/2026; {@code branchId} rỗng = toàn chuỗi. */
    Long membership(Long memberId, String type, Long branchId, String status, Integer remaining) {
        return membership(memberId, type, branchId, status, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31),
            remaining);
    }

    Long membership(Long memberId, String type, Long branchId, String status, LocalDate start, LocalDate end,
            Integer remaining) {
        Long pkg = b.f.sale.pkg(type + "_CK" + (++seq), type, "TIME".equals(type) ? null : 10, 500_000, branchId,
            "ACTIVE");
        return b.f.sale.membership(memberId, pkg, type, branchId, status, start, end, remaining);
    }

    /** Buổi PT của hội viên, bắt đầu {@code startVn}, dài 60 phút. */
    Long ptSession(Long memberId, Long membershipId, Long branchId, LocalDateTime startVn, String status) {
        OffsetDateTime start = BookingFixtures.utc(startVn);
        return jdbc.queryForObject("""
            INSERT INTO pt_sessions (trainer_id, member_id, membership_id, branch_id, start_at, end_at, status,
                                     commission_amount, cancelled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id""", Long.class,
            ptTrainer, memberId, membershipId, branchId, start, start.plusHours(1), status,
            "DONE".equals(status) ? 100_000 : null, "CANCELLED".equals(status) ? start : null);
    }

    Integer remaining(Long membershipId) {
        return jdbc.queryForObject("SELECT remaining_sessions FROM memberships WHERE id = ?", Integer.class,
            membershipId);
    }

    String status(Long membershipId) {
        return jdbc.queryForObject("SELECT status FROM memberships WHERE id = ?", String.class, membershipId);
    }

    LocalDate startDate(Long membershipId) {
        return jdbc.queryForObject("SELECT start_date FROM memberships WHERE id = ?", LocalDate.class, membershipId);
    }

    LocalDate endDate(Long membershipId) {
        return jdbc.queryForObject("SELECT end_date FROM memberships WHERE id = ?", LocalDate.class, membershipId);
    }

    long checkins(Long memberId) {
        return jdbc.queryForObject("SELECT count(*) FROM checkins WHERE member_id = ?", Long.class, memberId);
    }

    long deductions(Long membershipId) {
        return jdbc.queryForObject("SELECT count(*) FROM checkins WHERE membership_id = ? AND session_deducted",
            Long.class, membershipId);
    }

    OffsetDateTime arrivedAt(Long bookingId) {
        return jdbc.queryForObject("SELECT arrived_at FROM class_bookings WHERE id = ?", OffsetDateTime.class,
            bookingId);
    }

    OffsetDateTime bookingUpdatedAt(Long bookingId) {
        return jdbc.queryForObject("SELECT updated_at FROM class_bookings WHERE id = ?", OffsetDateTime.class,
            bookingId);
    }

    RequestPostProcessor reception() {
        return b.bearer(receptionist, UserRole.RECEPTIONIST);
    }

    RequestPostProcessor as(Long userId, UserRole role) {
        return b.bearer(userId, role);
    }
}
