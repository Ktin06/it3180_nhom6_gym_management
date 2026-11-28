package vn.edu.hust.nhom6.gym.feature.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.edu.hust.nhom6.gym.feature.notification.entity.Notification;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationChannel;

/** Repository JPA cho bảng {@code notifications} (F10). */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Danh sách thông báo của một tài khoản theo kênh, mới nhất trước (UC-F10.5). */
    Page<Notification> findByUserIdAndChannelOrderByCreatedAtDescIdDesc(
            Long userId, NotificationChannel channel, Pageable pageable);

    /** Đếm số thông báo chưa đọc của một tài khoản theo kênh (dùng chỉ mục {@code (user_id, read_at)}). */
    long countByUserIdAndChannelAndReadAtIsNull(Long userId, NotificationChannel channel);

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
