package vn.edu.hust.nhom6.gym.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.config.SchedulingConfig;

/** Công tắc {@code app.scheduling.enabled} bật/tắt cả bộ lập lịch lẫn các lớp {@link ScheduledJob}. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SchedulingConfigTest {

    @Autowired
    private ApplicationContext appContext;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(SchedulingConfig.class, SampleJob.class);

    @Test
    @DisplayName("Nền tảng: cấu hình kiểm thử tắt tác vụ nền trong toàn bộ ứng dụng")
    void kiemThuTatTacVuNen() {
        assertThat(appContext.getBeanNamesForType(ScheduledAnnotationBeanPostProcessor.class)).isEmpty();
    }

    @Test
    @DisplayName("Nền tảng: không khai báo công tắc thì tác vụ nền bật (mặc định của môi trường chạy thật)")
    void macDinhBat() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(ctx).hasSingleBean(SampleJob.class);
        });
    }

    @Test
    @DisplayName("Nền tảng: TaskScheduler của Spring Boot có một luồng, chạy bù và cron không chồng nhau")
    void boLapLichMotLuong() {
        runner.withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class)).run(ctx -> {
            assertThat(ctx).hasSingleBean(TaskScheduler.class);
            assertThat(ctx.getBean(ThreadPoolTaskScheduler.class).getScheduledThreadPoolExecutor().getCorePoolSize())
                .isEqualTo(1);
        });
    }

    @Test
    @DisplayName("Nền tảng: app.scheduling.enabled=false thì không có bộ lập lịch, không tạo lớp tác vụ")
    void tatThiKhongTaoTacVu() {
        runner.withPropertyValues("app.scheduling.enabled=false").run(ctx -> {
            assertThat(ctx).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(ctx).doesNotHaveBean(SampleJob.class);
        });
    }

    @ScheduledJob
    static class SampleJob {
    }
}
