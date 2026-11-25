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

    boolean existsByEmail(String email);
}
