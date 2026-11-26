package vn.edu.hust.nhom6.gym.feature.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Kind;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Slot;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository.BranchHours;
import vn.edu.hust.nhom6.gym.feature.notification.service.ClassSessionEventListener;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class ClassSessionEventListenerTest {

    @Mock
    private NotificationLookupRepository lookup;

    @Mock
    private NotificationService notificationService;

    private ClassSessionEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ClassSessionEventListener(lookup, notificationService);
    }

    @Test
    @DisplayName("UC-F10.4, ADR 0014: sự kiện đổi giờ lớp tạo CLASS_RESCHEDULED cho hội viên có tài khoản và bỏ qua người chưa có tài khoản")
    void suKienDoiGioTaoThongBaoChoHoiVienCoTaiKhoan() {
        var before = new Slot(1L, OffsetDateTime.parse("2026-11-27T02:00:00Z"), OffsetDateTime.parse("2026-11-27T03:00:00Z"));
        var after = new Slot(1L, OffsetDateTime.parse("2026-11-27T04:00:00Z"), OffsetDateTime.parse("2026-11-27T05:00:00Z"));
        var event = new ClassSessionChanged(
            Kind.TIME_CHANGED, 50L, 1L, 2L, 3L,
            OffsetDateTime.parse("2026-11-26T02:00:00Z"), before, after, List.of(101L, 102L));

        when(lookup.findMemberUserIds(List.of(101L, 102L))).thenReturn(Map.of(101L, 1001L));
        when(lookup.findBranchHours(1L)).thenReturn(Optional.of(new BranchHours(LocalTime.of(5, 30), LocalTime.of(22, 0))));
        when(lookup.findSessionClassName(50L)).thenReturn(Optional.of("Yoga cơ bản"));

        listener.onClassSessionChanged(event);

        verify(notificationService).createPair(
            eq(1001L), eq(NotificationType.CLASS_RESCHEDULED), anyString(), contains("09:00 ngày 27/11/2026"),
            eq("class_sessions"), eq(50L), eq(LocalTime.of(5, 30)), eq(LocalTime.of(22, 0)));
        verify(notificationService, never()).createPair(
            eq(102L), any(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("ADR 0014: sự kiện đổi phòng và hủy buổi tạo đúng loại thông báo")
    void suKienDoiPhongVaHuyBuoi() {
        var before = new Slot(1L, OffsetDateTime.parse("2026-11-27T02:00:00Z"), OffsetDateTime.parse("2026-11-27T03:00:00Z"));
        var after = new Slot(2L, OffsetDateTime.parse("2026-11-27T02:00:00Z"), OffsetDateTime.parse("2026-11-27T03:00:00Z"));
        when(lookup.findMemberUserIds(List.of(101L))).thenReturn(Map.of(101L, 1001L));
        when(lookup.findBranchHours(1L)).thenReturn(Optional.of(new BranchHours(LocalTime.of(5, 30), LocalTime.of(22, 0))));
        when(lookup.findSessionClassName(50L)).thenReturn(Optional.of("Zumba"));
        when(lookup.findRoomName(1L)).thenReturn(Optional.of("Phòng A"));
        when(lookup.findRoomName(2L)).thenReturn(Optional.of("Phòng B"));

        listener.onClassSessionChanged(new ClassSessionChanged(
            Kind.ROOM_CHANGED, 50L, 1L, 2L, 3L, OffsetDateTime.parse("2026-11-26T02:00:00Z"),
            before, after, List.of(101L)));
        listener.onClassSessionChanged(new ClassSessionChanged(
            Kind.CANCELLED, 50L, 1L, 2L, 3L, OffsetDateTime.parse("2026-11-26T02:00:00Z"),
            before, before, List.of(101L)));

        verify(notificationService).createPair(
            eq(1001L), eq(NotificationType.CLASS_ROOM_CHANGED), anyString(), contains("Phòng B"),
            eq("class_sessions"), eq(50L), any(), any());
        verify(notificationService).createPair(
            eq(1001L), eq(NotificationType.CLASS_CANCELLED), anyString(), contains("đã bị hủy"),
            eq("class_sessions"), eq(50L), any(), any());
    }

    @Test
    @DisplayName("UC-F10.4 A1: buổi lớp chưa có hội viên đặt chỗ thì không tạo thông báo")
    void khongCoHoiVienKhongTaoThongBao() {
        var slot = new Slot(1L, OffsetDateTime.parse("2026-11-27T02:00:00Z"), OffsetDateTime.parse("2026-11-27T03:00:00Z"));
        listener.onClassSessionChanged(new ClassSessionChanged(
            Kind.CANCELLED, 50L, 1L, 2L, 3L, OffsetDateTime.parse("2026-11-26T02:00:00Z"),
            slot, slot, List.of()));
        verifyNoInteractions(lookup, notificationService);
    }

    @Test
    @DisplayName("ADR 0014: lỗi khi tạo thông báo không được ném ngược làm hỏng thao tác F8")
    void loiThongBaoKhongLamHongF8() {
        var slot = new Slot(1L, OffsetDateTime.parse("2026-11-27T02:00:00Z"), OffsetDateTime.parse("2026-11-27T03:00:00Z"));
        when(lookup.findMemberUserIds(List.of(101L))).thenReturn(Map.of(101L, 1001L));
        when(lookup.findBranchHours(1L)).thenReturn(Optional.empty());
        when(lookup.findSessionClassName(50L)).thenReturn(Optional.of("Yoga"));
        doThrow(new RuntimeException("DB lỗi")).when(notificationService)
            .createPair(any(), any(), anyString(), anyString(), anyString(), any(), any(), any());

        listener.onClassSessionChanged(new ClassSessionChanged(
            Kind.CANCELLED, 50L, 1L, 2L, 3L, OffsetDateTime.parse("2026-11-26T02:00:00Z"),
            slot, slot, List.of(101L)));
    }
}
