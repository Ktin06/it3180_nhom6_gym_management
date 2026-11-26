package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationResponse;
import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationChannel;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationStatus;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationRealtimePublisher;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationRealtimePublisher realtimePublisher;

    private VnTime vnTime;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        // 2026-11-26 09:00 giờ VN = 02:00 UTC
        vnTime = new VnTime(Clock.fixed(Instant.parse("2026-11-26T02:00:00Z"), ZoneOffset.UTC));
        service = new NotificationService(repository, realtimePublisher, vnTime);
    }

    @Test
    @DisplayName("BR-F10.1: createPair lưu đủ hai kênh IN_APP (SENT) và EMAIL (PENDING) rồi đẩy WebSocket")
    void createPairLuuHaiKenhVaDayWebSocket() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse result = service.createPair(
            15L,
            NotificationType.MEMBERSHIP_EXPIRING,
            "Gói tập sắp hết hạn",
            "Gói 1 tháng sắp hết hạn.",
            "memberships",
            99L,
            LocalTime.of(5, 30),
            LocalTime.of(22, 0)
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(saved.get(0).getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.get(1).getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(saved.get(1).getStatus()).isEqualTo(NotificationStatus.PENDING);

        verify(realtimePublisher).pushToUser(eq(15L), eq(result));
    }

    @Test
    @DisplayName("UC-F10.5: hội viên xem danh sách thông báo IN_APP của chính mình")
    void xemDanhSachThongBaoInApp() {
        CurrentUser member = new CurrentUser(15L, UserRole.MEMBER);
        Notification n = Notification.inApp(
            15L, NotificationType.CLASS_REMINDER, "Nhắc lịch lớp", "Yoga 10:00",
            "class_bookings", 5L, vnTime.now());
        when(repository.findByUserIdAndChannelOrderByCreatedAtDescIdDesc(
            15L, NotificationChannel.IN_APP, PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(List.of(n)));

        var page = service.listMyNotifications(member, 0, 20);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().type()).isEqualTo("CLASS_REMINDER");
        assertThat(page.content().getFirst().readAt()).isNull();
    }

    @Test
    @DisplayName("BR-F10.7, BR-F10.8: đánh dấu đã đọc ghi read_at; gọi lại không đổi read_at")
    void danhDauDaDocGhiReadAtVaIdempotent() {
        CurrentUser member = new CurrentUser(15L, UserRole.MEMBER);
        Notification n = Notification.inApp(
            15L, NotificationType.CLASS_RESCHEDULED, "Đổi giờ", "Nội dung",
            "class_sessions", 7L, vnTime.now());
        when(repository.findByIdAndChannel(10L, NotificationChannel.IN_APP)).thenReturn(Optional.of(n));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse first = service.markAsRead(member, 10L);
        assertThat(first.readAt()).isEqualTo(OffsetDateTime.parse("2026-11-26T02:00:00Z"));

        NotificationResponse second = service.markAsRead(member, 10L);
        assertThat(second.readAt()).isEqualTo(first.readAt());
    }

    @Test
    @DisplayName("BR-F10.7: đánh dấu đã đọc thông báo của người khác trả 403 Forbidden")
    void danhDauDaDocThongBaoNguoiKhacTra403() {
        CurrentUser otherMember = new CurrentUser(99L, UserRole.MEMBER);
        Notification n = Notification.inApp(
            15L, NotificationType.CLASS_REMINDER, "Nhắc lớp", "Nội dung",
            "class_bookings", 5L, vnTime.now());
        when(repository.findByIdAndChannel(10L, NotificationChannel.IN_APP)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.markAsRead(otherMember, 10L))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("UC-F10.5 E3: đánh dấu đã đọc thông báo không tồn tại trả 404 NotFound")
    void danhDauDaDocKhongTonTaiTra404() {
        CurrentUser member = new CurrentUser(15L, UserRole.MEMBER);
        when(repository.findByIdAndChannel(404L, NotificationChannel.IN_APP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(member, 404L))
            .isInstanceOf(NotFoundException.class);
    }
}
