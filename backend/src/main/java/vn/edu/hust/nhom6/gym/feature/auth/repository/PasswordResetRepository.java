package vn.edu.hust.nhom6.gym.feature.auth.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import vn.edu.hust.nhom6.gym.feature.auth.entity.PasswordReset;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByTokenHash(String tokenHash);

    /**
     * Tra cứu {@code userId} từ {@code tokenHash} mà không tải entity vào persistence context,
     * phục vụ khóa dòng {@code users} trước khi khóa dòng {@code password_resets} để tránh deadlock.
     */
    @Query("SELECT pr.user.id FROM PasswordReset pr WHERE pr.tokenHash = :tokenHash")
    Optional<Long> findUserIdByTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Khóa dòng mã đặt lại mật khẩu theo {@code tokenHash} ({@code SELECT ... FOR UPDATE})
     * để ngăn hai yêu cầu đặt lại mật khẩu đồng thời sử dụng cùng một token dùng một lần (UC-F1.5).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pr FROM PasswordReset pr WHERE pr.tokenHash = :tokenHash")
    Optional<PasswordReset> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /**
     * Đánh dấu {@code used_at = :now} cho mọi mã đặt lại mật khẩu chưa dùng của tài khoản {@code userId}
     * trong cùng giao dịch đặt lại mật khẩu, vô hiệu hóa các liên kết "Quên mật khẩu" cũ khác (UC-F1.5).
     */
    @Modifying
    @Query("UPDATE PasswordReset pr SET pr.usedAt = :now WHERE pr.user.id = :userId AND pr.usedAt IS NULL")
    void markAllUnusedAsUsedByUserId(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}
