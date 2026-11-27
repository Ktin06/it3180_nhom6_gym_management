package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.service.ClassReminderService;
import vn.edu.hust.nhom6.gym.feature.notification.service.MembershipExpiryReminderService;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService.CreatedNotifications;

/**
 * Kiểm thử tích hợp S2-21 (#179): {@link NotificationService#create}, nhắc gói sắp hết (UC-F10.1),
 * nhắc lịch lớp (UC-F10.2) trên PostgreSQL Testcontainers với {@link MutableClock}.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class NotificationReminderIntegrationTest {
    static final LocalDate TODAY = LocalDate.of(2026, 11, 21);

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired VnTime time;
    @Autowired NotificationService notifications;
    @Autowired MembershipExpiryReminderService membershipReminders;
    @Autowired ClassReminderService classReminders;

    NotificationFixtures f;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 0, 10));
        f = new NotificationFixtures(jdbc, tokens);
        f.reset();
    }

    @Test
    @DisplayName("TC-F10.1.1-06a: create tạo hai dòng, IN_APP đã có trong web (SENT), EMAIL PENDING gửi ngay khi đang mở cửa")
    void createMakesTwoRows() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        Account a = f.account(f.cn01);

        CreatedNotifications created = notifications.create(a.userId(), NotificationType.CLASS_CANCELLED,
            "Buổi lớp bị hủy", "Nội dung", NotificationRelatedEntity.CLASS_SESSIONS, 99L, f.cn01);

        assertThat(created.inApp().getId()).isNotNull();
        assertThat(created.email().getId()).isNotNull();
        List<Map<String, Object>> rows = f.rows("class_sessions", 99L);
        assertThat(rows).hasSize(2);
        Map<String, Object> inApp = rows.get(0);
        Map<String, Object> email = rows.get(1);
        assertThat(inApp).containsEntry("channel", "IN_APP").containsEntry("status", "SENT")
            .containsEntry("type", "CLASS_CANCELLED").containsEntry("user_id", a.userId());
        assertThat(inApp.get("scheduled_at")).isNull();
        assertThat(inApp.get("read_at")).isNull();
        assertThat(instant(inApp.get("sent_at"))).isEqualTo(time.now().toInstant());
        assertThat(email).containsEntry("channel", "EMAIL").containsEntry("status", "PENDING")
            .containsEntry("title", "Buổi lớp bị hủy").containsEntry("content", "Nội dung");
        assertThat(email.get("sent_at")).isNull();
        assertThat(instant(email.get("scheduled_at"))).isEqualTo(time.now().toInstant());
    }

    @Test
    @DisplayName("TC-F10.1.1-07a: tạo lúc 23:00 thì email hẹn giờ mở cửa hôm sau của đúng chi nhánh, dòng trong web có ngay")
    void createAfterCloseSchedulesNextOpening() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 23, 0));
        Account a = f.account(f.cn01);

        notifications.create(a.userId(), NotificationType.CLASS_CANCELLED, "Hủy", null,
            NotificationRelatedEntity.CLASS_SESSIONS, 1L, f.cn02);

        List<Map<String, Object>> rows = f.rows("class_sessions", 1L);
        assertThat(rows.get(0)).containsEntry("status", "SENT");
        assertThat(instant(rows.get(1).get("scheduled_at")))
            .isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 22, 6, 0)).toInstant());
    }

    @Test
    @DisplayName("TC-F10.1.1-06b: create từ chối tham số sai (thiếu chi nhánh, tiêu đề rỗng, đối tượng liên quan thiếu id)")
    void createRejectsInvalidArguments() {
        Account a = f.account(f.cn01);
        assertThatThrownBy(() -> notifications.create(a.userId(), NotificationType.CLASS_CANCELLED, "T", null,
            null, null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> notifications.create(a.userId(), NotificationType.CLASS_CANCELLED, " ", null,
            null, null, f.cn01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> notifications.create(a.userId(), NotificationType.CLASS_CANCELLED, "T", null,
            NotificationRelatedEntity.CLASS_SESSIONS, null, f.cn01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> notifications.create(a.userId(), NotificationType.CLASS_CANCELLED, "T", null,
            null, null, 999_999L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(f.count()).isZero();
    }

    @Test
    @DisplayName("TC-F10.1.1-01a: gói ACTIVE hết hạn trong [hôm nay, hôm nay+7) được nhắc hai dòng; hết hạn đúng ngày +7, gói EXPIRED, UPCOMING thì không")
    void expiringWithinSevenDays() {
        Account soon = f.account(f.cn01);
        Account today = f.account(f.cn01);
        Account edge = f.account(f.cn01);
        Account expired = f.account(f.cn01);
        Long soonId = f.membership(soon.memberId(), "TIME", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(6), null);
        Long todayId = f.membership(today.memberId(), "TIME", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY, null);
        Long edgeId = f.membership(edge.memberId(), "TIME", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(7), null);
        Long expiredId = f.membership(expired.memberId(), "TIME", f.cn01, "EXPIRED", TODAY.minusDays(30), TODAY.minusDays(1), null);

        List<Long> reminded = membershipReminders.run(TODAY);

        assertThat(reminded).containsExactlyInAnyOrder(soonId, todayId);
        List<Map<String, Object>> rows = f.rows("memberships", soonId);
        assertThat(rows).extracting(r -> r.get("channel")).containsExactly("IN_APP", "EMAIL");
        assertThat(rows).allSatisfy(r -> assertThat(r)
            .containsEntry("type", "MEMBERSHIP_EXPIRING")
            .containsEntry("user_id", soon.userId())
            .containsEntry("title", "Gói tập sắp hết hạn"));
        assertThat((String) rows.get(0).get("content")).contains("hết hạn sau 6 ngày (27/11/2026)");
        assertThat((String) f.rows("memberships", todayId).get(0).get("content")).contains("hết hạn hôm nay");
        assertThat(instant(rows.get(1).get("scheduled_at")))
            .isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 5, 30)).toInstant());
        assertThat(f.rows("memberships", edgeId)).isEmpty();
        assertThat(f.rows("memberships", expiredId)).isEmpty();
    }

    @Test
    @DisplayName("TC-F10.1.1-02a: gói SESSION còn ≤ ngưỡng và chưa quá hạn được nhắc; hơn ngưỡng hoặc đã quá end_date thì không")
    void sessionThresholdFromConfig() {
        f.branchThreshold(f.cn02, 3);
        Account two = f.account(f.cn01);
        Account three = f.account(f.cn01);
        Account threeCn02 = f.account(f.cn02);
        Account zero = f.account(f.cn01);
        Account overdueSession = f.account(f.cn01);
        Long twoId = f.membership(two.memberId(), "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(60), 2);
        Long threeId = f.membership(three.memberId(), "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(60), 3);
        Long threeCn02Id = f.membership(threeCn02.memberId(), "SESSION", f.cn02, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(60), 3);
        Long zeroId = f.membership(zero.memberId(), "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(5), TODAY.plusDays(60), 0);
        Long overdueId = f.membership(overdueSession.memberId(), "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(30), TODAY.minusDays(1), 1);

        List<Long> reminded = membershipReminders.run(TODAY);

        assertThat(reminded).containsExactlyInAnyOrder(twoId, threeCn02Id, zeroId);
        assertThat(f.rows("memberships", threeId)).isEmpty();
        assertThat(f.rows("memberships", overdueId)).isEmpty();
        Map<String, Object> inApp = f.rows("memberships", twoId).get(0);
        assertThat(inApp).containsEntry("title", "Gói tập sắp hết buổi");
        assertThat((String) inApp.get("content")).contains("còn 2 buổi");
        assertThat(instant(f.rows("memberships", threeCn02Id).get(1).get("scheduled_at")))
            .isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 6, 0)).toInstant());
    }

    @Test
    @DisplayName("TC-F10.1.1-04a: đã có gói UPCOMING cùng loại cùng chi nhánh thì không nhắc; UPCOMING khác loại vẫn nhắc")
    void upcomingSameTypeSkips() {
        Account renewed = f.account(f.cn01);
        Account otherType = f.account(f.cn01);
        Long renewedId = f.membership(renewed.memberId(), "TIME", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(3), null);
        f.membership(renewed.memberId(), "TIME", f.cn01, "UPCOMING", TODAY.plusDays(4), TODAY.plusDays(33), null);
        Long otherId = f.membership(otherType.memberId(), "TIME", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(3), null);
        f.membership(otherType.memberId(), "SESSION", f.cn01, "UPCOMING", TODAY.plusDays(4), TODAY.plusDays(33), 10);

        assertThat(membershipReminders.run(TODAY)).containsExactly(otherId);
        assertThat(f.rows("memberships", renewedId)).isEmpty();
    }

    @Test
    @DisplayName("TC-F10.1.1-05a: hội viên chưa có tài khoản thì bỏ qua, không lỗi")
    void memberWithoutAccountSkipped() {
        Long memberId = f.memberWithoutAccount(f.cn01);
        Long pkg = f.membership(memberId, "TIME", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(2), null);

        assertThat(membershipReminders.run(TODAY)).isEmpty();
        assertThat(f.rows("memberships", pkg)).isEmpty();
    }

    @Test
    @DisplayName("TC-F10.1.1-03a: chạy lặp (cùng ngày và hôm sau) không tạo thêm thông báo cho cùng gói")
    void rerunDoesNotDuplicate() {
        Account a = f.account(f.cn01);
        Long pkg = f.membership(a.memberId(), "SESSION", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(5), 1);

        assertThat(membershipReminders.run(TODAY)).containsExactly(pkg);
        assertThat(membershipReminders.run(TODAY)).isEmpty();
        clock.advance(Duration.ofDays(1));
        assertThat(membershipReminders.run(TODAY.plusDays(1))).isEmpty();
        assertThat(f.rows("memberships", pkg)).hasSize(2);
        assertThat(f.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-F10.1.1-01b: gói toàn chuỗi (branch_id rỗng) tính theo chi nhánh gốc hội viên, email hẹn theo giờ chi nhánh đó")
    void chainWideMembershipUsesHomeBranch() {
        Account a = f.account(f.cn02);
        Long pkg = f.membership(a.memberId(), "TIME", null, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(1), null);

        assertThat(membershipReminders.run(TODAY)).containsExactly(pkg);
        assertThat(instant(f.rows("memberships", pkg).get(1).get("scheduled_at")))
            .isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 21, 6, 0)).toInstant());
    }

    @Test
    @DisplayName("TC-F10.1.1-01f: lỗi cấu hình ở một chi nhánh không hủy nhắc các chi nhánh khác và báo lỗi để tác vụ thử lại")
    void branchConfigErrorDoesNotRollbackOtherBranch() {
        Account a1 = f.account(f.cn01);
        Account a2 = f.account(f.cn02);
        Long pkg1 = f.membership(a1.memberId(), "TIME", f.cn01, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(2), null);
        Long pkg2 = f.membership(a2.memberId(), "TIME", f.cn02, "ACTIVE", TODAY.minusDays(20), TODAY.plusDays(2), null);
        jdbc.update("INSERT INTO system_configs (branch_id, config_key, config_value) VALUES (?, 'SESSION_WARNING_THRESHOLD', 'khong-phai-so')",
            f.cn02);

        assertThatThrownBy(() -> membershipReminders.run(TODAY))
            .isInstanceOf(IllegalStateException.class);
        assertThat(f.rows("memberships", pkg1)).hasSize(2);
        assertThat(f.rows("memberships", pkg2)).isEmpty();

        jdbc.update("UPDATE system_configs SET config_value = '2' WHERE branch_id = ? AND config_key = 'SESSION_WARNING_THRESHOLD'",
            f.cn02);
        assertThat(membershipReminders.run(TODAY)).containsExactly(pkg2);
        assertThat(f.rows("memberships", pkg1)).hasSize(2);
        assertThat(f.rows("memberships", pkg2)).hasSize(2);
    }

    @Test
    @DisplayName("TC-F10.2.1-01a: chỗ BOOKED của buổi bắt đầu trong 24 giờ tới được nhắc hai dòng; buổi còn hơn 24 giờ thì chờ lượt sau")
    void classWithin24Hours() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        Account a = f.account(f.cn01);
        Long soon = f.session(f.cn01, LocalDateTime.of(2026, 11, 22, 9, 0), "SCHEDULED");
        Long later = f.session(f.cn01, LocalDateTime.of(2026, 11, 22, 10, 30), "SCHEDULED");
        Long soonBooking = f.booking(soon, a.memberId(), "BOOKED");
        Long laterBooking = f.booking(later, a.memberId(), "BOOKED");

        assertThat(classReminders.run(time.now())).containsExactly(soonBooking);
        List<Map<String, Object>> rows = f.rows("class_bookings", soonBooking);
        assertThat(rows).extracting(r -> r.get("channel")).containsExactly("IN_APP", "EMAIL");
        assertThat(rows.get(0)).containsEntry("type", "CLASS_REMINDER").containsEntry("user_id", a.userId())
            .containsEntry("title", "Nhắc lịch lớp Yoga");
        assertThat((String) rows.get(0).get("content")).contains("09:00 ngày 22/11/2026");

        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 30));
        assertThat(classReminders.run(time.now())).containsExactly(laterBooking);
    }

    @Test
    @DisplayName("TC-F10.2.1-01b: tên lớp nhắc lấy theo class_sessions.class_type_id dù khung lịch class_schedules đổi sang loại lớp khác")
    void classReminderUsesSessionClassTypeNotSchedule() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        Account a = f.account(f.cn01);
        Long session = f.session(f.cn01, LocalDateTime.of(2026, 11, 22, 9, 0), "SCHEDULED");
        Long booking = f.booking(session, a.memberId(), "BOOKED");
        Long pilatesTypeId = jdbc.queryForObject(
            "INSERT INTO class_types (name, description, duration_minutes, is_active) VALUES ('Pilates', 'Mới', 60, true) RETURNING id",
            Long.class);
        jdbc.update("""
            UPDATE class_schedules SET class_type_id = ?
            WHERE id = (SELECT class_schedule_id FROM class_sessions WHERE id = ?)""",
            pilatesTypeId, session);

        assertThat(classReminders.run(time.now())).containsExactly(booking);
        List<Map<String, Object>> rows = f.rows("class_bookings", booking);
        assertThat(rows.get(0)).containsEntry("title", "Nhắc lịch lớp Yoga");
        assertThat((String) rows.get(0).get("content")).contains("Lớp Yoga");
    }

    @Test
    @DisplayName("TC-F10.2.1-03a: chỗ đã hủy, buổi đã hủy, buổi đã bắt đầu, hội viên chưa có tài khoản thì không nhắc; buổi đã đổi giờ vẫn nhắc")
    void cancelledOrStartedNotReminded() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        Account a = f.account(f.cn01);
        Account b = f.account(f.cn01);
        Long open = f.session(f.cn01, LocalDateTime.of(2026, 11, 21, 18, 0), "SCHEDULED");
        Long cancelledSession = f.session(f.cn01, LocalDateTime.of(2026, 11, 21, 19, 0), "CANCELLED");
        Long started = f.session(f.cn01, LocalDateTime.of(2026, 11, 21, 9, 30), "SCHEDULED");
        Long rescheduled = f.session(f.cn01, LocalDateTime.of(2026, 11, 21, 20, 0), "RESCHEDULED");
        Long cancelledBooking = f.booking(open, a.memberId(), "CANCELLED");
        Long inCancelledSession = f.booking(cancelledSession, a.memberId(), "BOOKED");
        Long inStarted = f.booking(started, a.memberId(), "BOOKED");
        Long inRescheduled = f.booking(rescheduled, a.memberId(), "BOOKED");
        Long okB = f.booking(open, b.memberId(), "BOOKED");
        Long noAccount = f.booking(open, f.memberWithoutAccount(f.cn01), "BOOKED");

        assertThat(classReminders.run(time.now())).containsExactlyInAnyOrder(okB, inRescheduled);
        assertThat(f.rows("class_bookings", cancelledBooking)).isEmpty();
        assertThat(f.rows("class_bookings", inCancelledSession)).isEmpty();
        assertThat(f.rows("class_bookings", inStarted)).isEmpty();
        assertThat(f.rows("class_bookings", noAccount)).isEmpty();
    }

    @Test
    @DisplayName("TC-F10.2.1-02a: chạy lặp mỗi 15 phút chỉ nhắc mỗi chỗ đúng một lần")
    void classReminderOnce() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        Account a = f.account(f.cn01);
        Long booking = f.booking(f.session(f.cn01, LocalDateTime.of(2026, 11, 21, 18, 0), "SCHEDULED"),
            a.memberId(), "BOOKED");

        assertThat(classReminders.run(time.now())).containsExactly(booking);
        clock.advance(Duration.ofMinutes(15));
        assertThat(classReminders.run(time.now())).isEmpty();
        clock.advance(Duration.ofHours(6));
        assertThat(classReminders.run(time.now())).isEmpty();
        assertThat(f.rows("class_bookings", booking)).hasSize(2);
    }

    static Instant instant(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        return ((OffsetDateTime) value).toInstant();
    }
}
