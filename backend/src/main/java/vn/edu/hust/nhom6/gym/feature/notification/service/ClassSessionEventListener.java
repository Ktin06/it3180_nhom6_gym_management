package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Kind;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository.BranchHours;

/**
 * Nhận sự kiện thay đổi buổi lớp nhóm từ F8 ({@link ClassSessionChanged}) và tạo thông báo trong
 * web + email cho các hội viên bị ảnh hưởng (F10, S2-22, ADR 0014).
 *
 * <p>Chạy ở giao dịch riêng ({@code REQUIRES_NEW}) sau khi giao dịch F8 đã commit; lỗi thông báo
 * được bắt và ghi log, không bao giờ làm hỏng thao tác đổi giờ / đổi phòng / hủy buổi của F8.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassSessionEventListener {

    public static final String ENTITY_CLASS_SESSIONS = "class_sessions";
    private static final DateTimeFormatter VN_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final NotificationLookupRepository lookup;
    private final NotificationService notificationService;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onClassSessionChanged(ClassSessionChanged event) {
        if (event == null || event.affectedMemberIds().isEmpty()) {
            return;
        }
        try {
            Map<Long, Long> userIdByMemberId = lookup.findMemberUserIds(event.affectedMemberIds());
            if (userIdByMemberId.isEmpty()) {
                return;
            }

            BranchHours hours = lookup.findBranchHours(event.branchId())
                .orElse(new BranchHours(BranchWindowScheduler.DEFAULT_OPEN_TIME, BranchWindowScheduler.DEFAULT_CLOSE_TIME));
            LocalTime openTime = hours.openTime();
            LocalTime closeTime = hours.closeTime();
            String className = lookup.findSessionClassName(event.sessionId()).orElse("Lớp học nhóm");

            NotificationType type = mapType(event.kind());
            String title = buildTitle(event.kind());
            String content = buildContent(event, className);

            for (Long memberId : event.affectedMemberIds()) {
                Long userId = userIdByMemberId.get(memberId);
                if (userId == null) {
                    continue;
                }
                notificationService.createPair(
                    userId,
                    type,
                    title,
                    content,
                    ENTITY_CLASS_SESSIONS,
                    event.sessionId(),
                    openTime,
                    closeTime
                );
            }
        } catch (RuntimeException ex) {
            log.error("Lỗi khi xử lý sự kiện {} cho buổi lớp id={}: {}",
                event.kind(), event.sessionId(), ex.getMessage(), ex);
        }
    }

    private static NotificationType mapType(Kind kind) {
        return switch (kind) {
            case TIME_CHANGED -> NotificationType.CLASS_RESCHEDULED;
            case ROOM_CHANGED -> NotificationType.CLASS_ROOM_CHANGED;
            case CANCELLED -> NotificationType.CLASS_CANCELLED;
        };
    }

    private static String buildTitle(Kind kind) {
        return switch (kind) {
            case TIME_CHANGED -> "Thông báo đổi giờ lớp học";
            case ROOM_CHANGED -> "Thông báo đổi phòng học";
            case CANCELLED -> "Thông báo hủy buổi lớp học";
        };
    }

    private String buildContent(ClassSessionChanged event, String className) {
        String beforeTime = formatTime(event.before().startAt());
        return switch (event.kind()) {
            case TIME_CHANGED -> String.format(
                "Buổi lớp %s đã được đổi giờ từ %s sang %s. Chỗ đặt của bạn vẫn được giữ nguyên; nếu bạn hủy buổi này sẽ không bị tính hủy muộn.",
                className, beforeTime, formatTime(event.after().startAt()));
            case ROOM_CHANGED -> {
                String beforeRoom = lookup.findRoomName(event.before().roomId())
                    .orElse("phòng #" + event.before().roomId());
                String afterRoom = lookup.findRoomName(event.after().roomId())
                    .orElse("phòng #" + event.after().roomId());
                yield String.format(
                    "Buổi lớp %s lúc %s đã đổi phòng từ %s sang %s.",
                    className, beforeTime, beforeRoom, afterRoom);
            }
            case CANCELLED -> String.format(
                "Buổi lớp %s lúc %s đã bị hủy. Chỗ đặt của bạn đã được hủy và không tính vi phạm.",
                className, beforeTime);
        };
    }

    private static String formatTime(OffsetDateTime instant) {
        return instant.atZoneSameInstant(VnTime.ZONE).format(VN_TIME_FMT);
    }
}
