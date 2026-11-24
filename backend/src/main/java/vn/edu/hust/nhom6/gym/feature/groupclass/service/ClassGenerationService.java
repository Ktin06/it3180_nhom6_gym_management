package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassGenerationService {
    private final ClassRepository classes;
    private final ClassScheduleService schedules;
    private final VnTime time;
    @Value("${app.classes.generation.days:14}") private int days;

    /** Mỗi lịch có giao dịch riêng, lịch lỗi được chạy lại ở lượt kế tiếp. */
    public void generateUpcomingSessions() {
        if (days < 1 || days > 90) {
            throw new IllegalStateException("Số ngày sinh buổi phải từ 1 đến 90.");
        }
        var from = time.today();
        for (long id : classes.activeScheduleIds()) {
            try {
                log.info("Sinh buổi lịch {}: {} buổi mới", id, schedules.generate(id, from, from.plusDays(days)));
            } catch (RuntimeException ex) {
                log.error("Không sinh được buổi cho lịch {}; thử lại lượt tiếp theo", id, ex);
            }
        }
    }
}
