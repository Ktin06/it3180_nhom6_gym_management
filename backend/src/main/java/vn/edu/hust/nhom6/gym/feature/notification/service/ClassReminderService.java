package vn.edu.hust.nhom6.gym.feature.notification.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository;
import vn.edu.hust.nhom6.gym.feature.notification.repository.NotificationQueryRepository.ClassReminderRow;

/**
 * Nghiệp vụ nhắc lịch lớp học nhóm đã đặt (UC-F10.2, BR-F10.4): nhắc các chỗ {@code BOOKED} của buổi
 * {@code SCHEDULED}/{@code RESCHEDULED} bắt đầu trong vòng 24 giờ tới, mỗi chỗ chỉ nhắc một lần.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassReminderService {
    public static final Duration REMIND_BEFORE = Duration.ofHours(24);
    static final DateTimeFormatter START_FORMATTER = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final NotificationQueryRepository queries;
    private final NotificationService notifications;

    /**
     * Chạy một lượt quét nhắc lịch lớp tại thời điểm {@code now}.
     *
     * @return danh sách mã lượt đặt chỗ vừa được nhắc
     */
    @Transactional
    public List<Long> run(OffsetDateTime now) {
        queries.lockForRun(NotificationQueryRepository.LOCK_CLASS_REMINDER);
        List<Long> reminded = new ArrayList<>();
        for (ClassReminderRow row : queries.findClassReminders(now, now.plus(REMIND_BEFORE))) {
            notifications.create(
                row.userId(),
                NotificationType.CLASS_REMINDER,
                "Nhắc lịch lớp " + row.className(),
                buildContent(row),
                NotificationRelatedEntity.CLASS_BOOKINGS,
                row.bookingId(),
                row.branchId());
            reminded.add(row.bookingId());
        }
        if (!reminded.isEmpty()) {
            log.info("Nhắc lịch lớp lúc {}: {} chỗ", now, reminded.size());
        }
        return reminded;
    }

    static String buildContent(ClassReminderRow row) {
        return "Lớp " + row.className() + " bắt đầu lúc "
            + row.startAt().atZoneSameInstant(VnTime.ZONE).format(START_FORMATTER) + " tại " + row.roomName()
            + ", " + row.branchName()
            + ". Nếu không tham gia được, vui lòng hủy chỗ sớm để nhường cho hội viên khác.";
    }
}
