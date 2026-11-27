package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository.ExpiringMembershipRow;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/**
 * Nghiệp vụ nhắc gói tập sắp hết hạn (trong 7 ngày tới) hoặc gói {@code SESSION} sắp hết buổi
 * (còn không quá {@code SESSION_WARNING_THRESHOLD}, CONFIG-F10.1).
 *
 * <p>Mỗi gói chỉ nhắc một lần (BR-F10.2) và dùng khóa tư vấn PostgreSQL để hai lượt chạy chồng không tạo trùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipExpiryReminderService {
    public static final int EXPIRING_WINDOW_DAYS = 7;
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificationQueryRepository queries;
    private final NotificationService notifications;
    private final SystemConfigService configs;

    /**
     * Chạy một lượt quét nhắc gói cho ngày Việt Nam {@code today}.
     *
     * @return danh sách mã gói vừa được tạo thông báo nhắc
     */
    @Transactional
    public List<Long> run(LocalDate today) {
        queries.lockForRun(NotificationQueryRepository.LOCK_MEMBERSHIP_REMINDER);
        LocalDate expireBefore = today.plusDays(EXPIRING_WINDOW_DAYS);
        List<Long> reminded = new ArrayList<>();
        for (Long branchId : queries.findBranchIds()) {
            int threshold = configs.getInt(branchId, SystemConfigKeys.SESSION_WARNING_THRESHOLD);
            for (ExpiringMembershipRow row : queries.findExpiringForReminder(branchId, today, expireBefore, threshold)) {
                boolean expiring = !row.endDate().isBefore(today) && row.endDate().isBefore(expireBefore);
                notifications.create(
                    row.userId(),
                    NotificationType.MEMBERSHIP_EXPIRING,
                    expiring ? "Gói tập sắp hết hạn" : "Gói tập sắp hết buổi",
                    buildContent(row, today, expiring),
                    NotificationRelatedEntity.MEMBERSHIPS,
                    row.membershipId(),
                    row.branchId());
                reminded.add(row.membershipId());
            }
        }
        log.info("Nhắc gói sắp hết ngày {}: {} gói", today, reminded.size());
        return reminded;
    }

    static String buildContent(ExpiringMembershipRow row, LocalDate today, boolean expiring) {
        StringBuilder text = new StringBuilder("Gói \"").append(row.packageName()).append("\" của bạn ");
        if (expiring) {
            long days = ChronoUnit.DAYS.between(today, row.endDate());
            text.append(days == 0 ? "hết hạn hôm nay (" : "hết hạn sau " + days + " ngày (")
                .append(row.endDate().format(DATE_FORMATTER)).append(")");
        }
        if ("SESSION".equals(row.packageType()) && row.remainingSessions() != null) {
            text.append(expiring ? ", " : "").append("còn ").append(row.remainingSessions()).append(" buổi");
        }
        return text.append(". Vui lòng gia hạn tại quầy lễ tân hoặc trên cổng hội viên.").toString();
    }
}
