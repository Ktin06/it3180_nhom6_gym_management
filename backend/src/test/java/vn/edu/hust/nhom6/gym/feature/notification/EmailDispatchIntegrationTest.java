package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.service.ClassReminderService;
import vn.edu.hust.nhom6.gym.feature.notification.service.EmailDispatchService;
import vn.edu.hust.nhom6.gym.feature.notification.service.EmailDispatchService.DispatchResult;
import vn.edu.hust.nhom6.gym.feature.notification.service.MembershipExpiryReminderService;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;

/**
 * Kiểm thử tích hợp tác vụ gửi email S2-21 (#179): {@link EmailDispatchService#dispatchDue} trên
 * PostgreSQL Testcontainers với {@link MutableClock} và {@link EmailSender} giả.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class EmailDispatchIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired VnTime time;
    @Autowired NotificationService notifications;
    @Autowired EmailDispatchService dispatcher;
    @Autowired ClassReminderService classReminders;
    @Autowired MembershipExpiryReminderService membershipReminders;

    @MockitoBean EmailSender emailSender;

    NotificationFixtures f;
    Account member;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 10, 0));
        f = new NotificationFixtures(jdbc, tokens);
        f.reset();
        member = f.account(f.cn01);
    }

    private void create(long relatedId) {
        notifications.create(member.userId(), NotificationType.CLASS_CANCELLED, "Buổi lớp bị hủy",
            "Buổi Yoga 18:00 ngày 21/11/2026 đã bị hủy.", NotificationRelatedEntity.CLASS_SESSIONS, relatedId, f.cn01);
    }

    private Map<String, Object> email(long relatedId) {
        return f.rows("class_sessions", relatedId).get(1);
    }

    @Test
    @DisplayName("US-F10.1.1 BR-F10.3: tạo lúc 23:00, chưa tới giờ hẹn thì không gửi; tới 05:30 hôm sau thì gửi, dòng EMAIL thành SENT")
    void sendsOnlyWhenDue() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 23, 0));
        create(1L);

        clock.setVn(LocalDateTime.of(2026, 11, 21, 23, 30));
        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(0, 0, 0));
        verifyNoInteractions(emailSender);

        clock.setVn(LocalDateTime.of(2026, 11, 22, 5, 30));
        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(1, 0, 0));
        verify(emailSender).send("hv" + member.userId() + "@gym-test.vn", "Buổi lớp bị hủy",
            "Buổi Yoga 18:00 ngày 21/11/2026 đã bị hủy.");
        Map<String, Object> row = email(1L);
        assertThat(row).containsEntry("status", "SENT");
        assertThat(NotificationReminderIntegrationTest.instant(row.get("sent_at"))).isEqualTo(time.now().toInstant());

        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(0, 0, 0));
    }

    @Test
    @DisplayName("US-F10.1.1 BR-F10.3: dòng đã tới hạn nhưng lúc gửi đã ngoài giờ mở cửa (máy chủ tắt) thì không gửi, hẹn lại giờ mở cửa kế tiếp")
    void outsideHoursAtDispatchReschedules() {
        create(2L);

        clock.setVn(LocalDateTime.of(2026, 11, 21, 22, 30));
        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(0, 0, 1));
        verifyNoInteractions(emailSender);
        Map<String, Object> row = email(2L);
        assertThat(row).containsEntry("status", "PENDING");
        assertThat(NotificationReminderIntegrationTest.instant(row.get("scheduled_at")))
            .isEqualTo(NotificationFixtures.utc(LocalDateTime.of(2026, 11, 22, 5, 30)).toInstant());
    }

    @Test
    @DisplayName("US-F10.1.1 E1: gửi lỗi thì dòng EMAIL thành FAILED, không thử lại, dòng trong web vẫn còn")
    void sendErrorMarksFailed() {
        doThrow(new IllegalStateException("Brevo 500")).when(emailSender).send(anyString(), anyString(), anyString());
        create(3L);

        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(0, 1, 0));
        List<Map<String, Object>> rows = f.rows("class_sessions", 3L);
        assertThat(rows.get(0)).containsEntry("channel", "IN_APP").containsEntry("status", "SENT");
        assertThat(rows.get(1)).containsEntry("channel", "EMAIL").containsEntry("status", "FAILED");
        assertThat(rows.get(1).get("sent_at")).isNull();
        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(0, 0, 0));
    }

    @Test
    @DisplayName("US-F10.1.1 ADR0013: ngoài profile local không gửi tới địa chỉ @example.com, dòng EMAIL thành FAILED; tài khoản DISABLED cũng không gửi")
    void blockedAddressAndDisabledAccount() {
        jdbc.update("UPDATE users SET email = 'hv-mau@example.com' WHERE id = ?", member.userId());
        create(4L);
        Account disabled = f.account(f.cn01);
        jdbc.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", disabled.userId());
        notifications.create(disabled.userId(), NotificationType.CLASS_CANCELLED, "Hủy", "x",
            NotificationRelatedEntity.CLASS_SESSIONS, 5L, f.cn01);

        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(0, 2, 0));
        verify(emailSender, never()).send(eq("hv-mau@example.com"), anyString(), anyString());
        verifyNoInteractions(emailSender);
        assertThat(email(4L)).containsEntry("status", "FAILED");
        assertThat(email(5L)).containsEntry("status", "FAILED");
    }

    @Test
    @DisplayName("US-F10.2.1 E5: email nhắc lớp đang chờ giờ mở cửa thì không gửi nếu hội viên đã hủy chỗ, buổi bị hủy hoặc lớp đã bắt đầu")
    void classReminderRecheckedAtDispatch() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 23, 0));
        Account b = f.account(f.cn01);
        Account c = f.account(f.cn01);
        Long session = f.session(f.cn01, LocalDateTime.of(2026, 11, 22, 7, 0), "SCHEDULED");
        Long early = f.session(f.cn01, LocalDateTime.of(2026, 11, 22, 5, 15), "SCHEDULED");
        Long cancelledLater = f.booking(session, member.memberId(), "BOOKED");
        Long kept = f.booking(session, b.memberId(), "BOOKED");
        Long beforeOpen = f.booking(early, c.memberId(), "BOOKED");
        assertThat(classReminders.run(time.now())).containsExactlyInAnyOrder(cancelledLater, kept, beforeOpen);

        jdbc.update("UPDATE class_bookings SET status = 'CANCELLED', cancelled_at = now(), cancel_reason = 'BY_MEMBER' "
            + "WHERE id = ?", cancelledLater);

        clock.setVn(LocalDateTime.of(2026, 11, 22, 5, 30));
        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(1, 2, 0));
        assertThat(f.rows("class_bookings", kept).get(1)).containsEntry("status", "SENT");
        assertThat(f.rows("class_bookings", cancelledLater).get(1)).containsEntry("status", "FAILED");
        assertThat(f.rows("class_bookings", beforeOpen).get(1)).containsEntry("status", "FAILED");
        assertThat(f.rows("class_bookings", cancelledLater).get(0)).containsEntry("status", "SENT");
    }

    @Test
    @DisplayName("US-F10.1.1 E5: email nhắc gói đang chờ thì không gửi nếu hội viên đã gia hạn (có gói UPCOMING cùng loại)")
    void membershipReminderRecheckedAtDispatch() {
        clock.setVn(LocalDateTime.of(2026, 11, 21, 0, 10));
        LocalDate today = LocalDate.of(2026, 11, 21);
        Long pkg = f.membership(member.memberId(), "TIME", f.cn01, "ACTIVE", today.minusDays(20), today.plusDays(3), null);
        assertThat(membershipReminders.run(today)).containsExactly(pkg);
        f.membership(member.memberId(), "TIME", f.cn01, "UPCOMING", today.plusDays(4), today.plusDays(33), null);

        clock.setVn(LocalDateTime.of(2026, 11, 21, 5, 30));
        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(0, 1, 0));
        verifyNoInteractions(emailSender);
        assertThat(f.rows("memberships", pkg).get(1)).containsEntry("status", "FAILED");
    }

    @Test
    @DisplayName("US-F10.2.1 BR-F10.3: chi nhánh của nhắc lớp lấy từ buổi lớp, của nhắc gói lấy từ gói (gói toàn chuỗi theo chi nhánh gốc)")
    void branchDerivedFromRelatedEntity() {
        f.setHours(f.cn01, "05:30", "21:00");
        Account cn02Member = f.account(f.cn02);
        Long session = f.session(f.cn02, LocalDateTime.of(2026, 11, 22, 9, 0), "SCHEDULED");
        Long booking = f.booking(session, member.memberId(), "BOOKED");
        Long chainPkg = f.membership(cn02Member.memberId(), "TIME", null, "ACTIVE",
            LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 25), null);
        notifications.create(member.userId(), NotificationType.CLASS_REMINDER, "Nhắc", "x",
            NotificationRelatedEntity.CLASS_BOOKINGS, booking, f.cn02);
        notifications.create(cn02Member.userId(), NotificationType.MEMBERSHIP_EXPIRING, "Gói", "x",
            NotificationRelatedEntity.MEMBERSHIPS, chainPkg, f.cn02);
        create(6L);

        clock.setVn(LocalDateTime.of(2026, 11, 21, 21, 30));
        assertThat(dispatcher.dispatchDue(time.now())).isEqualTo(new DispatchResult(2, 0, 1));
        assertThat(f.rows("class_bookings", booking).get(1)).containsEntry("status", "SENT");
        assertThat(f.rows("memberships", chainPkg).get(1)).containsEntry("status", "SENT");
        assertThat(email(6L)).containsEntry("status", "PENDING");
    }
}
