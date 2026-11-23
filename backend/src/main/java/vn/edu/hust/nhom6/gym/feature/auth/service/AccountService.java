package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;

/**
 * Thao tác trên tài khoản ({@code users}) mà chức năng khác cần gọi (F1, ADR 0007).
 *
 * <p>Tách khỏi {@link AuthService} để không có vòng phụ thuộc: AuthService gọi F2 tạo hồ sơ khi đăng ký,
 * còn F2 gọi lớp này để lấy email, khóa hoặc mở lại tài khoản. Lớp này không phụ thuộc chức năng nào khác.
 * Mọi hàm ghi tham gia giao dịch của nơi gọi: lỗi ở nơi gọi thì thay đổi tài khoản cũng được hoàn tác.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /** Email theo danh sách id tài khoản; id không tồn tại thì không có trong kết quả. */
    @Transactional(readOnly = true)
    public Map<Long, String> getEmailsByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, User::getEmail));
    }

    /**
     * Ngừng hoạt động tài khoản (DISABLED) và thu hồi toàn bộ refresh token đang hiệu lực (UC-F1.6, UC-F2.4).
     *
     * @throws NotFoundException (404) khi không có tài khoản
     */
    @Transactional
    public void disableAccount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản liên kết."));
        user.setStatus(UserStatus.DISABLED);
        userRepository.saveAndFlush(user);
        refreshTokenRepository.revokeAllByUserId(userId, OffsetDateTime.now());
    }

    /** Kích hoạt lại tài khoản đang DISABLED; không mở tài khoản LOCKED (UC-F1.6, UC-F2.4). */
    @Transactional
    public void activateAccountIfDisabled(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getStatus() == UserStatus.DISABLED) {
                user.setStatus(UserStatus.ACTIVE);
                userRepository.saveAndFlush(user);
            }
        });
    }
}
