package vn.edu.hust.nhom6.gym.feature.trainer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import vn.edu.hust.nhom6.gym.feature.trainer.event.PtSessionCancelledByManagerEvent;

/**
 * Listener <b>giả</b> của phía thông báo (F10), chỉ có trong thư mục kiểm thử. Nghe giống cách F10 thật sẽ nghe
 * ({@code AFTER_COMMIT}); nhận được sự kiện cũng chứng minh việc hủy đã commit.
 */
@TestConfiguration
public class PtSessionEventProbe {

    @Bean
    Recorder ptSessionEventRecorder() {
        return new Recorder();
    }

    public static class Recorder {
        private final List<PtSessionCancelledByManagerEvent> events = new CopyOnWriteArrayList<>();

        @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        public void onCancelledByManager(PtSessionCancelledByManagerEvent event) {
            events.add(event);
        }

        public void reset() {
            events.clear();
        }

        public List<PtSessionCancelledByManagerEvent> events() {
            return List.copyOf(events);
        }
    }
}
