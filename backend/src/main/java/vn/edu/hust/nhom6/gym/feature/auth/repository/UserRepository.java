package vn.edu.hust.nhom6.gym.feature.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * Khóa dòng tài khoản theo {@code email} ({@code SELECT ... FOR UPDATE})
     * để nhiều yêu cầu đăng nhập đồng thời tăng đúng bộ đếm {@code failed_login_count} (S4).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);

    /**
     * Khóa dòng tài khoản theo {@code id} ({@code SELECT ... FOR UPDATE})
     * để các thao tác nhạy cảm (đặt lại mật khẩu và thu hồi phiên) không tranh chấp với luồng cấp phiên.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    /** Khóa tài khoản trước khi khóa dòng refresh token, cùng thứ tự với đổi và đặt lại mật khẩu. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = (SELECT r.user.id FROM RefreshToken r WHERE r.tokenHash = :tokenHash)")
    Optional<User> findByRefreshTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    boolean existsByEmail(String email);
}
