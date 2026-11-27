package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.SessionTimeRequest;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.ClassSessionService;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;

/**
 * Kiểm thử tích hợp listener F10 (S2-22, #180): nghe sự kiện {@code ClassSessionChanged} do
 * {@link ClassSessionService} của F8 phát sau khi commit và tạo thông báo độc lập cho từng hội viên.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class NotificationEventListenerIntegrationTest {
    static final LocalDateTime NOW_VN = LocalDateTime.of(2026, 11, 21, 10, 0);
    static final LocalDateTime OLD_START_VN = LocalDateTime.of(2026, 11, 22, 18, 0);
    static final LocalDateTime NEW_START_VN = LocalDateTime.of(2026, 11, 22, 19, 30);

    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired ClassSessionService classSessions;
    @MockitoSpyBean NotificationService notificationService;

    NotificationFixtures f;
    CurrentUser manager;

    @BeforeEach
    void setUp() {
        clock.setVn(NOW_VN);
        f = new NotificationFixtures(jdbc, tokens);
        f.reset();
        Long managerUserId = f.bookings.f.user("quan-ly-f10@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        manager = new CurrentUser(managerUserId, UserRole.BRANCH_MANAGER);
    }

    @AfterEach
    void tearDown() {
        reset(notificationService);
    }

    @Test
    @DisplayName("US-F10.4.1 KB1: đổi giờ buổi lớp tạo CLASS_RESCHEDULED (IN_APP và EMAIL) ghi giờ cũ, giờ mới theo giờ VN, không hạn xác nhận lại")
    void rescheduleCreatesNotificationsWithOldAndNewVietnamTimes() {
        Account hv1 = f.account(f.cn01);
        Account hv2 = f.account(f.cn01);
        Long sessionId = f.session(f.cn01, OLD_START_VN, "SCHEDULED");
        f.booking(sessionId, hv1.memberId(), "BOOKED");
        f.booking(sessionId, hv2.memberId(), "BOOKED");

        classSessions.changeTime(manager, sessionId, new SessionTimeRequest(
            NotificationFixtures.utc(NEW_START_VN), NotificationFixtures.utc(NEW_START_VN.plusMinutes(60))));

        List<Map<String, Object>> rows = f.rows("class_sessions", sessionId);
        assertThat(rows).hasSize(4);
        assertThat(rows).extracting(r -> r.get("type")).containsOnly("CLASS_RESCHEDULED");
        assertThat(rows).extracting(r -> r.get("user_id"))
            .containsExactlyInAnyOrder(hv1.userId(), hv1.userId(), hv2.userId(), hv2.userId());
        assertThat(rows.stream().filter(r -> "IN_APP".equals(r.get("channel"))).count()).isEqualTo(2);
        assertThat(rows.stream().filter(r -> "EMAIL".equals(r.get("channel"))).count()).isEqualTo(2);

        String content = (String) rows.getFirst().get("content");
        assertThat(content)
            .contains("Yoga")
            .contains("18:00")
            .contains("19:00")
            .contains("19:30")
            .contains("20:30")
            .contains("22/11/2026")
            .doesNotContain("xác nhận lại");
    }

    @Test
    @DisplayName("US-F10.4.1 KB2: đổi phòng buổi lớp tạo CLASS_ROOM_CHANGED ghi phòng cũ và phòng mới")
    void changeRoomCreatesNotificationsWithRoomNames() {
        Account hv = f.account(f.cn01);
        Long sessionId = f.session(f.cn01, OLD_START_VN, "SCHEDULED");
        f.booking(sessionId, hv.memberId(), "BOOKED");
        String oldRoomName = jdbc.queryForObject(
            "SELECT r.name FROM class_sessions cs JOIN rooms r ON r.id = cs.room_id WHERE cs.id = ?",
            String.class, sessionId);
        Long newRoomId = f.bookings.f.room(f.cn01, "Phòng Studio Mới", 40);

        classSessions.changeRoom(manager, sessionId, newRoomId);

        List<Map<String, Object>> rows = f.rows("class_sessions", sessionId);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.get("channel")).containsExactly("IN_APP", "EMAIL");
        assertThat(rows).allSatisfy(r -> assertThat(r)
            .containsEntry("type", "CLASS_ROOM_CHANGED")
            .containsEntry("user_id", hv.userId()));
        String content = (String) rows.getFirst().get("content");
        assertThat(content).contains(oldRoomName).contains("Phòng Studio Mới").contains("18:00 ngày 22/11/2026");
    }

    @Test
    @DisplayName("US-F10.4.1 KB3: hủy buổi lớp tạo CLASS_CANCELLED cho mọi hội viên có chỗ vừa bị hủy và có tài khoản")
    void cancelClassCreatesNotifications() {
        Account hv1 = f.account(f.cn01);
        Account hv2 = f.account(f.cn01);
        Long sessionId = f.session(f.cn01, OLD_START_VN, "SCHEDULED");
        f.booking(sessionId, hv1.memberId(), "BOOKED");
        f.booking(sessionId, hv2.memberId(), "BOOKED");

        classSessions.cancel(manager, sessionId);

        List<Map<String, Object>> rows = f.rows("class_sessions", sessionId);
        assertThat(rows).hasSize(4);
        assertThat(rows).extracting(r -> r.get("type")).containsOnly("CLASS_CANCELLED");
        assertThat(rows).extracting(r -> r.get("user_id"))
            .containsExactlyInAnyOrder(hv1.userId(), hv1.userId(), hv2.userId(), hv2.userId());
        assertThat((String) rows.getFirst().get("content"))
            .contains("Yoga")
            .contains("18:00 ngày 22/11/2026");
    }

    @Test
    @DisplayName("US-F10.4.1 A1: hội viên không có chỗ BOOKED hoặc chưa có tài khoản không nhận thông báo sự kiện lớp")
    void nonBookedOrNoAccountMembersReceiveNothing() {
        Account bookedMember = f.account(f.cn01);
        Account cancelledMember = f.account(f.cn01);
        Account unrelatedMember = f.account(f.cn01);
        Long noAccountMemberId = f.memberWithoutAccount(f.cn01);

        Long sessionId = f.session(f.cn01, OLD_START_VN, "SCHEDULED");
        f.booking(sessionId, bookedMember.memberId(), "BOOKED");
        f.booking(sessionId, cancelledMember.memberId(), "CANCELLED");
        f.booking(sessionId, noAccountMemberId, "BOOKED");

        classSessions.changeTime(manager, sessionId, new SessionTimeRequest(
            NotificationFixtures.utc(NEW_START_VN), NotificationFixtures.utc(NEW_START_VN.plusMinutes(60))));

        List<Map<String, Object>> classRows = f.rows("class_sessions", sessionId);
        assertThat(classRows).hasSize(2);
        assertThat(classRows).extracting(r -> r.get("user_id")).containsOnly(bookedMember.userId());
        assertThat(classRows).extracting(r -> r.get("user_id"))
            .doesNotContain(cancelledMember.userId(), unrelatedMember.userId());
    }

    @Test
    @DisplayName("US-F10.4.1 E1: lỗi khi tạo thông báo không rollback thao tác F8 và không chặn người nhận khác")
    void notificationFailureDoesNotRollbackF8OrBlockOtherRecipients() {
        Account hvFail = f.account(f.cn01);
        Account hvOk = f.account(f.cn01);
        Long sessionId = f.session(f.cn01, OLD_START_VN, "SCHEDULED");
        f.booking(sessionId, hvFail.memberId(), "BOOKED");
        f.booking(sessionId, hvOk.memberId(), "BOOKED");

        doThrow(new RuntimeException("Lỗi giả lập khi lưu thông báo"))
            .when(notificationService).create(eq(hvFail.userId()), any(), any(), any(), any(), any(), any());

        classSessions.changeTime(manager, sessionId, new SessionTimeRequest(
            NotificationFixtures.utc(NEW_START_VN), NotificationFixtures.utc(NEW_START_VN.plusMinutes(60))));

        Map<String, Object> sessionRow = jdbc.queryForMap(
            "SELECT status, start_at FROM class_sessions WHERE id = ?", sessionId);
        assertThat(sessionRow).containsEntry("status", "RESCHEDULED");
        assertThat(NotificationReminderIntegrationTest.instant(sessionRow.get("start_at")))
            .isEqualTo(NotificationFixtures.utc(NEW_START_VN).toInstant());

        List<Map<String, Object>> rows = f.rows("class_sessions", sessionId);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.get("user_id")).containsOnly(hvOk.userId());
    }
}
