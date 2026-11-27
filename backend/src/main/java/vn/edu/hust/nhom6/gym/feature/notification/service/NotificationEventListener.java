package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository.ClassSessionContext;

/**
 * Nhận sự kiện thay đổi buổi lớp nhóm ({@link ClassSessionChanged}) do F8 phát sau khi commit
 * ({@code TransactionSynchronization.afterCommit}), tạo thông báo {@code IN_APP} + {@code EMAIL} cho từng
 * hội viên liên quan trong giao dịch mới độc lập ({@code REQUIRES_NEW}) qua
 * {@link NotificationTransactionalCreator}.
 *
 * <p>Theo ADR 0014: không có {@code CLASS_PROMOTED}, không có hạn xác nhận lại. Mọi lỗi khi xử lý sự kiện
 * hoặc lưu thông báo cho một người đều được bắt và ghi log, không bao giờ rollback thao tác F8 hoặc chặn
 * các người nhận còn lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_DATE = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final NotificationQueryRepository queries;
    private final NotificationTransactionalCreator creator;

    @EventListener
    public void onClassSessionChanged(ClassSessionChanged event) {
        if (event == null || event.affectedMemberIds().isEmpty()) {
            return;
        }
        try {
            switch (event.kind()) {
                case TIME_CHANGED -> handleRescheduled(event);
                case ROOM_CHANGED -> handleRoomChanged(event);
                case CANCELLED -> handleCancelled(event);
            }
        } catch (Exception ex) {
            log.error("Lỗi xử lý sự kiện {} cho buổi lớp sessionId={}", event.kind(), event.sessionId(), ex);
        }
    }

    /** UC-F10.4 (ADR 0014): đổi giờ buổi lớp → ghi giờ cũ và giờ mới theo giờ Việt Nam, giữ nguyên chỗ. */
    private void handleRescheduled(ClassSessionChanged event) {
        List<Long> userIds = queries.findBookedMemberUserIds(event.sessionId(), event.affectedMemberIds());
        if (userIds.isEmpty()) {
            return;
        }
        ClassSessionContext ctx = queries.findClassSessionContext(event.sessionId()).orElse(null);
        String className = ctx != null ? ctx.className() : "lớp nhóm";
        String location = ctx != null ? " tại " + ctx.roomName() + ", " + ctx.branchName() : "";
        String title = "Lịch lớp " + className + " đã đổi giờ";
        String content = "Lớp " + className + location
            + " đã đổi giờ từ " + formatRangeVn(event.before().startAt(), event.before().endAt())
            + " sang " + formatRangeVn(event.after().startAt(), event.after().endAt())
            + ". Chỗ đặt của bạn vẫn được giữ nguyên.";

        notifyEach(userIds, NotificationType.CLASS_RESCHEDULED, title, content,
            NotificationRelatedEntity.CLASS_SESSIONS, event.sessionId(), event.branchId());
    }

    /** Đổi phòng buổi lớp → ghi tên phòng cũ, phòng mới và giờ học theo giờ Việt Nam. */
    private void handleRoomChanged(ClassSessionChanged event) {
        List<Long> userIds = queries.findBookedMemberUserIds(event.sessionId(), event.affectedMemberIds());
        if (userIds.isEmpty()) {
            return;
        }
        ClassSessionContext ctx = queries.findClassSessionContext(event.sessionId()).orElse(null);
        Map<Long, String> rooms = queries.findRoomNames(List.of(event.before().roomId(), event.after().roomId()));
        String className = ctx != null ? ctx.className() : "lớp nhóm";
        String branchPart = ctx != null ? " tại " + ctx.branchName() : "";
        String oldRoom = rooms.getOrDefault(event.before().roomId(), "phòng cũ");
        String newRoom = rooms.getOrDefault(event.after().roomId(), "phòng mới");
        String title = "Lớp " + className + " đã đổi phòng";
        String content = "Lớp " + className + " lúc " + formatInstantVn(event.after().startAt()) + branchPart
            + " đã đổi từ " + oldRoom + " sang " + newRoom + ". Chỗ đặt của bạn vẫn được giữ nguyên.";

        notifyEach(userIds, NotificationType.CLASS_ROOM_CHANGED, title, content,
            NotificationRelatedEntity.CLASS_SESSIONS, event.sessionId(), event.branchId());
    }

    /** Hủy buổi lớp → gửi cho hội viên có tài khoản nằm trong {@code affectedMemberIds} (chỗ đã chuyển CANCELLED). */
    private void handleCancelled(ClassSessionChanged event) {
        List<Long> userIds = queries.findMemberUserIds(event.affectedMemberIds());
        if (userIds.isEmpty()) {
            return;
        }
        ClassSessionContext ctx = queries.findClassSessionContext(event.sessionId()).orElse(null);
        String className = ctx != null ? ctx.className() : "lớp nhóm";
        String location = ctx != null ? " tại " + ctx.roomName() + ", " + ctx.branchName() : "";
        String title = "Buổi lớp " + className + " đã bị hủy";
        String content = "Buổi lớp " + className + " lúc " + formatInstantVn(event.before().startAt()) + location
            + " đã bị hủy. Chỗ đặt của bạn được hủy và không tính vi phạm.";

        notifyEach(userIds, NotificationType.CLASS_CANCELLED, title, content,
            NotificationRelatedEntity.CLASS_SESSIONS, event.sessionId(), event.branchId());
    }

    private void notifyEach(
            List<Long> userIds,
            NotificationType type,
            String title,
            String content,
            NotificationRelatedEntity relatedEntity,
            Long relatedId,
            Long branchId) {
        for (Long userId : userIds) {
            try {
                creator.createInNewTransaction(userId, type, title, content, relatedEntity, relatedId, branchId);
            } catch (Exception ex) {
                log.error("Lỗi tạo thông báo {} cho userId={}, {}={}",
                    type, userId, relatedEntity.table(), relatedId, ex);
            }
        }
    }

    private static String formatInstantVn(OffsetDateTime instant) {
        return instant.atZoneSameInstant(VnTime.ZONE).format(TIME_DATE);
    }

    private static String formatRangeVn(OffsetDateTime start, OffsetDateTime end) {
        ZonedDateTime startVn = start.atZoneSameInstant(VnTime.ZONE);
        ZonedDateTime endVn = end.atZoneSameInstant(VnTime.ZONE);
        if (startVn.toLocalDate().equals(endVn.toLocalDate())) {
            return startVn.format(TIME_ONLY) + "–" + endVn.format(TIME_ONLY) + " ngày " + startVn.format(DATE_ONLY);
        }
        return startVn.format(TIME_DATE) + " – " + endVn.format(TIME_DATE);
    }
}
