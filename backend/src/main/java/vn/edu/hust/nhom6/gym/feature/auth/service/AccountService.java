package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;

/**
 * Thao tác trên tài khoản ({@code users}) mà chức năng khác cần gọi (F1, ADR 0007).
 *
 * <p>Tách khỏi {@link AuthService} để không có vòng phụ thuộc: AuthService gọi F2 tạo hồ sơ khi đăng ký,
 * còn F2 gọi lớp này để lấy email, tạo tài khoản, khóa hoặc mở lại tài khoản. Lớp này không phụ thuộc chức năng nào khác.
 * Mọi hàm ghi tham gia giao dịch của nơi gọi: lỗi ở nơi gọi thì thay đổi tài khoản cũng được hoàn tác.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    public static final String EMAIL_ALREADY_USED_MESSAGE =
        "Email này đã được sử dụng bởi một tài khoản khác.";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

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

    /**
     * Tạo tài khoản hội viên ({@code role = MEMBER}, {@code status = ACTIVE}) với mật khẩu ngẫu nhiên
     * đã băm BCrypt (không ai biết) để gắn vào hồ sơ có sẵn (F2, UC-F2.2).
     *
     * <p>Tham gia giao dịch của nơi gọi; lỗi sau đó ở nơi gọi sẽ hoàn tác cả bản ghi {@code users}.
     *
     * @throws ConflictException (409) khi email đã thuộc tài khoản khác (UC-F2.2 E2)
     */
    @Transactional
    public Long createMemberAccount(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException(EMAIL_ALREADY_USED_MESSAGE);
        }

        // Mật khẩu ngẫu nhiên 32 byte (~43 ký tự Base64URL, dưới 72 byte BCrypt); không lưu dạng thô và không trả ra ngoài.
        String randomPassword = TokenHashUtil.generateRawToken();
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(randomPassword))
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .failedLoginCount(0)
            .build();
        try {
            User saved = userRepository.saveAndFlush(user);
            return saved.getId();
        } catch (DataIntegrityViolationException ex) {
            String cause = String.valueOf(ex.getMostSpecificCause().getMessage());
            if (cause.contains("uq_users_email")) {
                throw new ConflictException(EMAIL_ALREADY_USED_MESSAGE);
            }
            throw ex;
        }
    }
}
