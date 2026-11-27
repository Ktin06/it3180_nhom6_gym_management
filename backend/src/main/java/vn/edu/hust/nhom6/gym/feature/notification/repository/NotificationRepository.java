package vn.edu.hust.nhom6.gym.feature.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;

/** Repository JPA cho bảng {@code notifications} (F10). */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Khóa bản ghi {@code EMAIL} còn {@code PENDING} bằng {@code FOR UPDATE SKIP LOCKED} để hai lượt gửi
     * chạy chồng không gửi trùng một email.
     */
    @Query(value = """
        SELECT * FROM notifications
        WHERE id = :id AND channel = 'EMAIL' AND status = 'PENDING'
        FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    Optional<Notification> lockPendingEmail(@Param("id") Long id);
}
