package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.mail.EmailSender;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.service.AccountService;
import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository.ExpiringMembershipRow;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationLookupRepository.UpcomingClassBookingRow;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationRepository;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/**
 * Nghiệp vụ các tác vụ nền của F10 (S2-21):
 * <ul>
 *   <li>Nhắc gói tập sắp hết hạn / sắp hết buổi (UC-F10.1, BR-F10.2, CONFIG-F10.1).</li>
 *   <li>Nhắc lịch lớp học nhóm đã đặt sắp diễn ra (UC-F10.2, BR-F10.4).</li>
 *   <li>Quét và gửi các thông báo kênh {@code EMAIL} đã đến {@code scheduled_at} (BR-F10.3).</li>
 * </ul>
 *
 * <p>Mọi hàm đều bất biến khi chạy lặp (idempotent): kiểm tra {@code related_entity}/{@code related_id}
 * để không tạo thông báo trùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationReminderService {

    public static final int EXPIRING_DAYS_WINDOW = 7;
    public static final String ENTITY_MEMBERSHIPS = "memberships";
    public static final String ENTITY_CLASS_BOOKINGS = "class_bookings";
    private static final int EMAIL_BATCH_SIZE = 50;
    private static final DateTimeFormatter VN_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter VN_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final NotificationLookupRepository lookup;
    private final NotificationRepository notifications;
    private final NotificationService notificationService;
    private final SystemConfigService systemConfigs;
    private final AccountService accounts;
    private final EmailSender emailSender;
    private final VnTime vnTime;

    @Value("${app.notification.class-reminder-hours:2}")
    private int classReminderHours = 2;

    /**
     * Quét gói tập ACTIVE sắp hết hạn (trong 7 ngày tới) hoặc gói SESSION ACTIVE còn không quá
     * {@code SESSION_WARNING_THRESHOLD} buổi; tạo cặp thông báo {@code IN_APP} + {@code EMAIL}.
     *
     * @return số gói vừa được tạo thông báo
     */
    @Transactional
    public int remindExpiringMemberships() {
        LocalDate today = vnTime.today();
        LocalDate maxEndDate = today.plusDays(EXPIRING_DAYS_WINDOW);
        int sessionThreshold = systemConfigs.getInt(null, SystemConfigKeys.SESSION_WARNING_THRESHOLD);

        List<ExpiringMembershipRow> candidates =
            lookup.findExpiringMemberships(today, maxEndDate, sessionThreshold);
        for (ExpiringMembershipRow row : candidates) {
            String title = "Gói tập sắp hết hạn";
            String content = buildExpiringContent(row, sessionThreshold);
            notificationService.createPair(
                row.userId(),
                NotificationType.MEMBERSHIP_EXPIRING,
                title,
                content,
                ENTITY_MEMBERSHIPS,
                row.membershipId(),
                row.branchOpenTime(),
                row.branchCloseTime()
            );
        }
        return candidates.size();
    }

    /**
     * Quét các lượt đặt chỗ {@code BOOKED} cho buổi lớp sắp bắt đầu trong vòng
     * {@code classReminderHours} giờ tới; tạo cặp thông báo {@code IN_APP} + {@code EMAIL}.
     *
     * @return số lượt đặt chỗ vừa được nhắc
     */
    @Transactional
    public int remindUpcomingClasses() {
        OffsetDateTime now = vnTime.now();
        OffsetDateTime remindUntil = now.plusHours(Math.max(1, classReminderHours));

        List<UpcomingClassBookingRow> candidates = lookup.findUpcomingBookedClasses(now, remindUntil);
        for (UpcomingClassBookingRow row : candidates) {
            String startLabel = row.startAt().atZoneSameInstant(VnTime.ZONE).format(VN_TIME_FMT);
            String title = "Nhắc lịch lớp học sắp diễn ra";
            String content = String.format(
                "Lớp %s tại phòng %s sẽ bắt đầu lúc %s. Vui lòng đến đúng giờ để check-in.",
                row.className(), row.roomName(), startLabel);
            notificationService.createPair(
                row.userId(),
                NotificationType.CLASS_REMINDER,
                title,
                content,
                ENTITY_CLASS_BOOKINGS,
                row.bookingId(),
                row.branchOpenTime(),
                row.branchCloseTime()
            );
        }
        return candidates.size();
    }

    /**
     * Quét các dòng {@code EMAIL} có {@code status = PENDING} và {@code scheduled_at <= now}, gửi
     * qua {@link EmailSender} ngoài giao dịch CSDL dài để không giữ kết nối HikariCP khi chờ HTTP;
     * cập nhật {@code SENT} kèm {@code sent_at} hoặc {@code FAILED} nếu lỗi (UC-F10.1 E1).
     *
     * @return số email gửi thành công
     */
    public int dispatchPendingEmails() {
        OffsetDateTime now = vnTime.now();
        List<Notification> pending = notifications.findDuePendingEmails(now, PageRequest.of(0, EMAIL_BATCH_SIZE));
        if (pending.isEmpty()) {
            return 0;
        }
        Set<Long> userIds = pending.stream()
            .map(Notification::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> emailsByUserId = accounts.getEmailsByUserIds(userIds);

        int sentCount = 0;
        for (Notification notification : pending) {
            String to = emailsByUserId.get(notification.getUserId());
            if (to == null || to.isBlank()) {
                notification.markFailed();
                notifications.save(notification);
                log.warn("Không tìm thấy email cho userId={}, đánh dấu FAILED thông báo id={}",
                    notification.getUserId(), notification.getId());
                continue;
            }
            try {
                emailSender.send(to, notification.getTitle(),
                    notification.getContent() != null ? notification.getContent() : notification.getTitle());
                notification.markSent(vnTime.now());
                notifications.save(notification);
                sentCount++;
            } catch (RuntimeException ex) {
                notification.markFailed();
                notifications.save(notification);
                log.error("Gửi email thông báo id={} (userId={}) thất bại: {}",
                    notification.getId(), notification.getUserId(), ex.getMessage());
            }
        }
        return sentCount;
    }

    private static String buildExpiringContent(ExpiringMembershipRow row, int sessionThreshold) {
        String endLabel = row.endDate().format(VN_DATE_FMT);
        if ("SESSION".equals(row.packageType())
                && row.remainingSessions() != null
                && row.remainingSessions() <= sessionThreshold) {
            return String.format(
                "Gói tập %s của bạn chỉ còn %d buổi (hạn dùng đến %s). Hãy gia hạn để không gián đoạn luyện tập.",
                row.packageName(), row.remainingSessions(), endLabel);
        }
        return String.format(
            "Gói tập %s của bạn sẽ hết hạn vào ngày %s. Hãy gia hạn sớm để không gián đoạn luyện tập.",
            row.packageName(), endLabel);
    }
}
