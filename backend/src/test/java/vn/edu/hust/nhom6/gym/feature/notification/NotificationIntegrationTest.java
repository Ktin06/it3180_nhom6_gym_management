package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Kind;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Slot;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationChannel;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationStatus;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationReminderService;

/**
 * Kiểm thử tích hợp (Testcontainers PostgreSQL) cho F10 (S2-21 & S2-22):
 * tác vụ nhắc gói sắp hết hạn, nhắc lịch lớp, hẹn gửi email theo giờ mở cửa,
 * nhận sự kiện đổi buổi từ F8 và API xem/đánh dấu đã đọc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class NotificationIntegrationTest {

    private static final String EMAIL_DOMAIN = "@f10-it.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private NotificationReminderService reminderService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private EmailSender emailSender;

    private long branchId;
    private long userA;
    private long userB;
    private long memberA;
    private long memberB;
    private long memberNoAccount;
    private long timePackageId;
    private long sessionPackageId;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 26, 9, 0));
        reset(emailSender);
        cleanTestData();

        branchId = jdbc.queryForObject("SELECT id FROM branches WHERE code = 'CN01'", Long.class);
        userA = insertUser("member-a" + EMAIL_DOMAIN);
        userB = insertUser("member-b" + EMAIL_DOMAIN);
        memberA = insertMember("HV880001", "0988800001", userA);
        memberB = insertMember("HV880002", "0988800002", userB);
        memberNoAccount = insertMember("HV880003", "0988800003", null);
        timePackageId = insertPackage("F10_TIME_PKG", "Gói tháng F10", "TIME", 30, null);
        sessionPackageId = insertPackage("F10_SESS_PKG", "Gói buổi F10", "SESSION", 60, 10);
    }

    @Test
    @DisplayName("S2-21 / UC-F10.1: nhắc gói TIME sắp hết hạn & SESSION còn <= 2 buổi, bỏ qua gói đã có UPCOMING, bỏ qua hội viên chưa có tài khoản, chạy lặp không gửi trùng")
    void nhacGoiSapHetHanVaChongGuiTrung() {
        LocalDate today = LocalDate.of(2026, 11, 26);
        long expiringTimeId = insertMembership(memberA, timePackageId, "TIME", today.minusDays(25), today.plusDays(5), null, null, "ACTIVE");
        long lowSessionId = insertMembership(memberA, sessionPackageId, "SESSION", today.minusDays(10), today.plusDays(30), 10, 2, "ACTIVE");
        // Hội viên B có gói sắp hết hạn nhưng đã có gói UPCOMING cùng loại -> không nhắc
        insertMembership(memberB, timePackageId, "TIME", today.minusDays(25), today.plusDays(3), null, null, "ACTIVE");
        insertMembership(memberB, timePackageId, "TIME", today.plusDays(4), today.plusDays(33), null, null, "UPCOMING");
        // Hội viên chưa có tài khoản -> không nhắc
        insertMembership(memberNoAccount, timePackageId, "TIME", today.minusDays(25), today.plusDays(2), null, null, "ACTIVE");

        int firstRun = reminderService.remindExpiringMemberships();
        int secondRun = reminderService.remindExpiringMemberships();

        assertThat(firstRun).isEqualTo(2);
        assertThat(secondRun).isZero();
        assertThat(notificationRepository.existsByTypeAndChannelAndRelatedEntityAndRelatedId(
            "MEMBERSHIP_EXPIRING", NotificationChannel.IN_APP, "memberships", expiringTimeId)).isTrue();
        assertThat(notificationRepository.existsByTypeAndChannelAndRelatedEntityAndRelatedId(
            "MEMBERSHIP_EXPIRING", NotificationChannel.EMAIL, "memberships", lowSessionId)).isTrue();

        int sent = reminderService.dispatchPendingEmails();
        assertThat(sent).isEqualTo(2);
        verify(emailSender, times(2)).send(eq("member-a" + EMAIL_DOMAIN), anyString(), anyString());
    }

    @Test
    @DisplayName("S2-21 / BR-F10.3: email phát sinh ngoài giờ mở cửa (23:00) được hẹn tới 05:30 hôm sau mới gửi")
    void emailNgoaiGioMoCuaHenDenGioMoCuaMoiGui() {
        clock.setVn(LocalDateTime.of(2026, 11, 26, 23, 0));
        LocalDate today = LocalDate.of(2026, 11, 26);
        insertMembership(memberA, timePackageId, "TIME", today.minusDays(25), today.plusDays(4), null, null, "ACTIVE");

        assertThat(reminderService.remindExpiringMemberships()).isEqualTo(1);
        // Lúc 23:00 chưa đến giờ mở cửa -> chưa gửi email
        assertThat(reminderService.dispatchPendingEmails()).isZero();

        // Sang 05:30 sáng hôm sau -> gửi thành công
        clock.setVn(LocalDateTime.of(2026, 11, 27, 5, 30));
        assertThat(reminderService.dispatchPendingEmails()).isEqualTo(1);
        verify(emailSender, times(1)).send(eq("member-a" + EMAIL_DOMAIN), anyString(), anyString());
    }

    @Test
    @DisplayName("S2-22 / UC-F10.4 & UC-F10.5: nhận sự kiện ClassSessionChanged từ F8, xem danh sách và đánh dấu đã đọc qua API")
    void nhanSuKienF8VaXemDanhDauDaDocQuaApi() throws Exception {
        OffsetDateTime oldStart = LocalDateTime.of(2026, 11, 27, 9, 0)
            .atZone(VnTime.ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime newStart = LocalDateTime.of(2026, 11, 27, 11, 0)
            .atZone(VnTime.ZONE).toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);

        eventPublisher.publishEvent(new ClassSessionChanged(
            Kind.TIME_CHANGED, 999L, branchId, 1L, userB,
            clock.instant().atOffset(ZoneOffset.UTC),
            new Slot(1L, oldStart, oldStart.plusHours(1)),
            new Slot(1L, newStart, newStart.plusHours(1)),
            List.of(memberA, memberNoAccount)
        ));

        var list = notificationRepository.findAll().stream()
            .filter(n -> n.getUserId().equals(userA) && n.getChannel() == NotificationChannel.IN_APP)
            .toList();
        assertThat(list).hasSize(1);
        long notifId = list.getFirst().getId();
        assertThat(list.getFirst().getStatus()).isEqualTo(NotificationStatus.SENT);

        // Hội viên A xem danh sách & số chưa đọc = 1
        mockMvc.perform(get("/api/v1/me/notifications")
                .with(jwt().jwt(j -> j.subject(String.valueOf(userA)).claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(notifId))
            .andExpect(jsonPath("$.content[0].type").value("CLASS_RESCHEDULED"))
            .andExpect(jsonPath("$.content[0].readAt").doesNotExist());

        mockMvc.perform(get("/api/v1/me/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(String.valueOf(userA)).claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));

        // Hội viên B cố đánh dấu thông báo của A -> 403
        mockMvc.perform(patch("/api/v1/me/notifications/" + notifId + "/read")
                .with(jwt().jwt(j -> j.subject(String.valueOf(userB)).claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isForbidden());

        // Hội viên A đánh dấu đã đọc -> 200, unreadCount = 0
        mockMvc.perform(patch("/api/v1/me/notifications/" + notifId + "/read")
                .with(jwt().jwt(j -> j.subject(String.valueOf(userA)).claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/me/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject(String.valueOf(userA)).claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0));
    }

    private void cleanTestData() {
        jdbc.update("DELETE FROM notifications WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)", "%" + EMAIL_DOMAIN);
        jdbc.update("DELETE FROM memberships WHERE member_id IN (SELECT id FROM members WHERE member_code IN ('HV880001','HV880002','HV880003'))");
        jdbc.update("DELETE FROM packages WHERE code IN ('F10_TIME_PKG','F10_SESS_PKG')");
        jdbc.update("DELETE FROM members WHERE member_code IN ('HV880001','HV880002','HV880003')");
        jdbc.update("DELETE FROM users WHERE email LIKE ?", "%" + EMAIL_DOMAIN);
    }

    private long insertUser(String email) {
        return jdbc.queryForObject("""
            INSERT INTO users (email, password_hash, role, status)
            VALUES (?, '$2a$10$dummyhashdummyhashdummyhashdummyhashdummyhashdummyha', 'MEMBER', 'ACTIVE')
            RETURNING id
            """, Long.class, email);
    }

    private long insertMember(String code, String phone, Long userId) {
        return jdbc.queryForObject("""
            INSERT INTO members (member_code, full_name, phone, home_branch_id, user_id, status)
            VALUES (?, 'Hội viên F10', ?, ?, ?, 'ACTIVE')
            RETURNING id
            """, Long.class, code, phone, branchId, userId);
    }

    private long insertPackage(String code, String name, String type, int durationDays, Integer sessions) {
        return jdbc.queryForObject("""
            INSERT INTO packages (code, name, type, duration_days, session_count, price, status)
            VALUES (?, ?, ?, ?, ?, 500000, 'ACTIVE')
            RETURNING id
            """, Long.class, code, name, type, durationDays, sessions);
    }

    private long insertMembership(long memberId, long packageId, String type, LocalDate start, LocalDate end,
            Integer totalSessions, Integer remainingSessions, String status) {
        return jdbc.queryForObject("""
            INSERT INTO memberships (member_id, package_id, package_type, branch_id, sold_branch_id,
                start_date, end_date, total_sessions, remaining_sessions, freeze_count, status, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 0)
            RETURNING id
            """, Long.class, memberId, packageId, type, branchId, branchId,
            start, end, totalSessions, remainingSessions, status);
    }
}
