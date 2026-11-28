package vn.edu.hust.nhom6.gym.feature.trainer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.hust.nhom6.gym.feature.trainer.event.PtSessionCancelledByPackageEndEvent;

/**
 * Listener <b>giả</b> của F10 cho sự kiện "buổi PT bị hủy vì gói hết buổi" (S2-19.3, GĐ-8). Nghe {@code AFTER_COMMIT}
 * như F10 thật sẽ nghe; chỉ có trong thư mục kiểm thử.
 */
@TestConfiguration
public class PtPackageEndEventProbe {

    @Bean
    Recorder ptPackageEndRecorder() {
        return new Recorder();
    }

    public static class Recorder {
        private final List<PtSessionCancelledByPackageEndEvent> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void on(PtSessionCancelledByPackageEndEvent event) {
            events.add(event);
        }

        public void reset() {
            events.clear();
        }

        public List<PtSessionCancelledByPackageEndEvent> events() {
            return List.copyOf(events);
        }
    }
}
