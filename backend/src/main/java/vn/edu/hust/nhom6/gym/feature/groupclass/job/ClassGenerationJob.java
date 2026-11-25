package vn.edu.hust.nhom6.gym.feature.groupclass.job;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import vn.edu.hust.nhom6.gym.common.scheduling.ScheduledJob;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.ClassGenerationService;

@ScheduledJob
@ConditionalOnProperty(prefix = "app.classes.generation", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ClassGenerationJob {
    private final ClassGenerationService generation;
    private final TaskScheduler scheduler;

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        scheduler.schedule(this::generate, Instant.now());
    }

    @Scheduled(cron = "${app.classes.generation.cron:0 5 * * * *}", zone = VnTime.ZONE_ID)
    public void generate() {
        generation.generateUpcomingSessions();
    }
}
