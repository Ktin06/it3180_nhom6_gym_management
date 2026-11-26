package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.service.AccountService;
import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationStatus;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository.ExpiringMembershipRow;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository.UpcomingClassBookingRow;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationReminderService;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

@ExtendWith(MockitoExtension.class)
class NotificationReminderServiceTest {

    @Mock
    private NotificationLookupRepository lookup;

    @Mock
    private NotificationRepository notifications;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SystemConfigService systemConfigs;

    @Mock
    private AccountService accounts;

    @Mock
    private EmailSender emailSender;

    private VnTime vnTime;
    private NotificationReminderService service;

    @BeforeEach
    void setUp() {
        // 2026-11-26 08:00 giờ VN = 01:00 UTC
        vnTime = new VnTime(Clock.fixed(Instant.parse("2026-11-26T01:00:00Z"), ZoneOffset.UTC));
        service = new NotificationReminderService(
            lookup, notifications, notificationService, systemConfigs, accounts, emailSender, vnTime);
    }

    @Test
    @DisplayName("UC-F10.1, CONFIG-F10.1: nhắc gói TIME sắp hết hạn và gói SESSION còn ít buổi theo SESSION_WARNING_THRESHOLD")
    void nhacGoiSapHetHanVaSapHetBuoi() {
        when(systemConfigs.getInt(null, SystemConfigKeys.SESSION_WARNING_THRESHOLD)).thenReturn(2);
        var timeRow = new ExpiringMembershipRow(
            101L, 10L, "Gói 1 tháng", "TIME", LocalDate.of(2026, 11, 30), null,
            LocalTime.of(5, 30), LocalTime.of(22, 0));
        var sessionRow = new ExpiringMembershipRow(
            102L, 11L, "Gói 10 buổi", "SESSION", LocalDate.of(2026, 12, 20), 2,
            LocalTime.of(5, 30), LocalTime.of(22, 0));
        when(lookup.findExpiringMemberships(LocalDate.of(2026, 11, 26), LocalDate.of(2026, 12, 3), 2))
            .thenReturn(List.of(timeRow, sessionRow));

        int count = service.remindExpiringMemberships();

        assertThat(count).isEqualTo(2);
        verify(notificationService).createPair(
            eq(10L), eq(NotificationType.MEMBERSHIP_EXPIRING), anyString(), contains("30/11/2026"),
            eq("memberships"), eq(101L), eq(LocalTime.of(5, 30)), eq(LocalTime.of(22, 0)));
        verify(notificationService).createPair(
            eq(11L), eq(NotificationType.MEMBERSHIP_EXPIRING), anyString(), contains("còn 2 buổi"),
            eq("memberships"), eq(102L), eq(LocalTime.of(5, 30)), eq(LocalTime.of(22, 0)));
    }

    @Test
    @DisplayName("UC-F10.2: nhắc lịch lớp đã đặt sắp bắt đầu")
    void nhacLichLopSapDienRa() {
        var row = new UpcomingClassBookingRow(
            55L, 7L, 10L, "Yoga cơ bản", "Phòng Studio 1",
            OffsetDateTime.parse("2026-11-26T02:30:00Z"),
            LocalTime.of(5, 30), LocalTime.of(22, 0));
        when(lookup.findUpcomingBookedClasses(any(), any())).thenReturn(List.of(row));

        int count = service.remindUpcomingClasses();

        assertThat(count).isEqualTo(1);
        verify(notificationService).createPair(
            eq(10L), eq(NotificationType.CLASS_REMINDER), anyString(), contains("Yoga cơ bản"),
            eq("class_bookings"), eq(55L), eq(LocalTime.of(5, 30)), eq(LocalTime.of(22, 0)));
    }

    @Test
    @DisplayName("UC-F10.1 E1: gửi email thành công chuyển SENT; lỗi gửi chuyển FAILED mà không chặn email khác")
    void guiEmailPendingChuyenSentHoacFailed() {
        Notification ok = Notification.email(
            10L, NotificationType.MEMBERSHIP_EXPIRING, "Tiêu đề 1", "Nội dung 1",
            "memberships", 1L, vnTime.now());
        Notification fail = Notification.email(
            11L, NotificationType.CLASS_REMINDER, "Tiêu đề 2", "Nội dung 2",
            "class_bookings", 2L, vnTime.now());
        when(notifications.findDuePendingEmails(any(), any(Pageable.class))).thenReturn(List.of(ok, fail));
        when(accounts.getEmailsByUserIds(Set.of(10L, 11L)))
            .thenReturn(Map.of(10L, "a@example.com", 11L, "b@example.com"));
        doNothing().when(emailSender).send(eq("a@example.com"), anyString(), anyString());
        doThrow(new RuntimeException("Brevo 503"))
            .when(emailSender).send(eq("b@example.com"), anyString(), anyString());

        int sent = service.dispatchPendingEmails();

        assertThat(sent).isEqualTo(1);
        assertThat(ok.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(ok.getSentAt()).isNotNull();
        assertThat(fail.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(fail.getSentAt()).isNull();
    }
}
