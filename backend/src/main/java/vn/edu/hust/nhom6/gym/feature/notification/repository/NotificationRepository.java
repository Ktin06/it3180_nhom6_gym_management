package vn.edu.hust.nhom6.gym.feature.notification.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationChannel;

/** Truy vấn bảng {@code notifications} (F10). */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDescIdDesc(
        Long userId, NotificationChannel channel, Pageable pageable);

    long countByUserIdAndChannelAndReadAtIsNull(Long userId, NotificationChannel channel);

    Optional<Notification> findByIdAndChannel(Long id, NotificationChannel channel);

    boolean existsByTypeAndChannelAndRelatedEntityAndRelatedId(
        String type, NotificationChannel channel, String relatedEntity, Long relatedId);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.channel = 'EMAIL'
          AND n.status = 'PENDING'
          AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now)
        ORDER BY n.scheduledAt ASC, n.id ASC
        """)
    List<Notification> findDuePendingEmails(@Param("now") OffsetDateTime now, Pageable pageable);
}
