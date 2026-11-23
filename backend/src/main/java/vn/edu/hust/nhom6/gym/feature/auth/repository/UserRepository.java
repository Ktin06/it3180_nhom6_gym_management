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
     * Khóa dòng tài khoản theo {@code id} ({@code SELECT ... FOR UPDATE})
     * để các thao tác nhạy cảm (đặt lại mật khẩu và thu hồi phiên) không tranh chấp với luồng cấp phiên.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    boolean existsByEmail(String email);
}
