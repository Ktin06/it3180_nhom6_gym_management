package vn.edu.hust.nhom6.gym.feature.membership.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tác vụ nền hằng ngày của gói hội viên (S2-07.2, AC-F3.5.1-10, BR-F4.7, BR-F3-11). Lớp riêng theo G13; việc đổi trạng
 * thái vẫn nằm trong {@link MembershipService}.
 *
 * <p>Chạy lặp nhiều lần trong cùng một ngày không đổi thêm gì: cả hai bước lọc theo trạng thái hiện tại, không cộng dồn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipLifecycleService {
    private final MembershipService memberships;

    /** Kết quả một lượt chạy, dùng cho log và kiểm thử. */
    public record DailyRun(LocalDate today, List<Long> expired, MembershipService.UpcomingActivation upcoming) { }

    /**
     * Một giao dịch, đúng thứ tự: (1) chuyển EXPIRED và flush; (2) kích hoạt UPCOMING có {@code start_date <= today}.
     * Đảo thứ tự thì gói mới lên ACTIVE trong khi gói cũ còn ACTIVE, vi phạm {@code uq_one_active_per_type}.
     *
     * @param today ngày Việt Nam do nơi gọi tính bằng {@code VnTime.today()} (tác vụ) hoặc {@code Clock} cố định (kiểm thử)
     */
    @Transactional
    public DailyRun runDaily(LocalDate today) {
        List<Long> expired = memberships.expireOverdue(today);
        MembershipService.UpcomingActivation upcoming = memberships.activateDueUpcoming(today);
        if (!upcoming.skipped().isEmpty()) {
            log.warn("Tác vụ gói {}: bỏ qua gói UPCOMING {} vì cùng nhóm còn gói ACTIVE/FROZEN", today,
                upcoming.skipped());
        }
        log.info("Tác vụ gói {}: {} gói EXPIRED, {} gói ACTIVE, {} gói UPCOMING quá hạn EXPIRED", today,
            expired.size(), upcoming.activated().size(), upcoming.expiredUnstarted().size());
        return new DailyRun(today, expired, upcoming);
    }
}
