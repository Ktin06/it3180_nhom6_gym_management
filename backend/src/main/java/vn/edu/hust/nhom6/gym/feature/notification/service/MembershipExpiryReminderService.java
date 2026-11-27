package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
 * <p>Mỗi chi nhánh chạy trong một giao dịch riêng kèm khóa tư vấn PostgreSQL để lỗi cấu hình hoặc lỗi tạm
 * thời ở một chi nhánh không hủy nhắc của các chi nhánh khác; các chi nhánh lỗi được báo lại để tác vụ nền
 * thử lại.
 */
@Slf4j
@Service
public class MembershipExpiryReminderService {
    public static final int EXPIRING_WINDOW_DAYS = 7;
    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificationQueryRepository queries;
    private final NotificationService notifications;
    private final SystemConfigService configs;
    private final TransactionTemplate tx;

    public MembershipExpiryReminderService(
            NotificationQueryRepository queries,
            NotificationService notifications,
            SystemConfigService configs,
            PlatformTransactionManager transactionManager) {
        this.queries = queries;
        this.notifications = notifications;
        this.configs = configs;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /**
     * Chạy một lượt quét nhắc gói cho ngày Việt Nam {@code today}, tách giao dịch theo từng chi nhánh.
     *
     * @return danh sách mã gói vừa được tạo thông báo nhắc
     */
    public List<Long> run(LocalDate today) {
        LocalDate expireBefore = today.plusDays(EXPIRING_WINDOW_DAYS);
        List<Long> reminded = new ArrayList<>();
        List<Long> failedBranches = new ArrayList<>();
        RuntimeException lastError = null;
        for (Long branchId : queries.findBranchIds()) {
            try {
                List<Long> branchReminded = tx.execute(status -> runBranch(branchId, today, expireBefore));
                if (branchReminded != null) {
                    reminded.addAll(branchReminded);
                }
            } catch (RuntimeException ex) {
                log.error("Nhắc gói sắp hết ngày {} tại chi nhánh {} gặp lỗi", today, branchId, ex);
                failedBranches.add(branchId);
                lastError = ex;
            }
        }
        log.info("Nhắc gói sắp hết ngày {}: {} gói ({} chi nhánh lỗi)", today, reminded.size(), failedBranches.size());
        if (!failedBranches.isEmpty()) {
            throw new IllegalStateException(
                "Nhắc gói sắp hết ngày " + today + " lỗi ở các chi nhánh " + failedBranches, lastError);
        }
        return reminded;
    }

    private List<Long> runBranch(Long branchId, LocalDate today, LocalDate expireBefore) {
        queries.lockForRun(NotificationQueryRepository.LOCK_MEMBERSHIP_REMINDER);
        int threshold = configs.getInt(branchId, SystemConfigKeys.SESSION_WARNING_THRESHOLD);
        List<Long> branchReminded = new ArrayList<>();
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
            branchReminded.add(row.membershipId());
        }
        return branchReminded;
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
