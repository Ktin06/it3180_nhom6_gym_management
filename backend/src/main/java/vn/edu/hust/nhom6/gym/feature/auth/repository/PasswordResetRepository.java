package vn.edu.hust.nhom6.gym.feature.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import vn.edu.hust.nhom6.gym.feature.auth.entity.PasswordReset;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByTokenHash(String tokenHash);

    /**
     * Khóa dòng mã đặt lại mật khẩu theo {@code tokenHash} ({@code SELECT ... FOR UPDATE})
     * để ngăn hai yêu cầu đặt lại mật khẩu đồng thời sử dụng cùng một token dùng một lần (UC-F1.5).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pr FROM PasswordReset pr WHERE pr.tokenHash = :tokenHash")
    Optional<PasswordReset> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
