package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;

/** Chạy bù khi khởi động và mỗi giờ; lỗi một lịch không ngăn lịch khác, lần sau thử lại. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassGenerationJob {
    private final ClassRepository classes;
    private final ClassScheduleService schedules;
    private final Clock classClock;
    @Value("${app.classes.generation.enabled:true}") private boolean enabled;
    @Value("${app.classes.generation.days:14}") private int days;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${app.classes.generation.cron:0 5 * * * *}", zone = "Asia/Ho_Chi_Minh")
    public void generate() {
        if (!enabled) { return; }
        if (days < 1 || days > 90) { throw new IllegalStateException("Số ngày sinh buổi phải từ 1 đến 90."); }
        var from = LocalDate.now(classClock.withZone(ClassScheduleService.VIETNAM));
        for (long id : classes.activeScheduleIds()) {
            try { log.info("Sinh buổi lịch {}: {} buổi mới", id, schedules.generate(id, from, from.plusDays(days))); }
            catch (RuntimeException ex) { log.error("Không sinh được buổi cho lịch {}; thử lại lượt tiếp theo", id, ex); }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class Scheduling {
        @Bean Clock classClock() { return Clock.systemUTC(); }
    }
}
