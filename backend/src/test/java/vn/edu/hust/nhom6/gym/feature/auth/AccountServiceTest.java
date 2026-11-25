package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;
import vn.edu.hust.nhom6.gym.feature.auth.service.AccountService;

/** Kiểm thử đơn vị cho {@link AccountService} (nền tảng N0.1 và S2-36). */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(userRepository, refreshTokenRepository, passwordEncoder);
    }

    @Test
    @DisplayName("TC-F1.6.1-04a, TC-F2.4.1-03a: disableAccount chuyển DISABLED và thu hồi mọi refresh token")
    void disableAccountCapNhatDisabledVaThuHoiRefreshToken() {
        User user = User.builder()
            .email("member@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.ACTIVE)
            .build();
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        accountService.disableAccount(10L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.DISABLED);
        verify(userRepository).saveAndFlush(user);
        verify(refreshTokenRepository).revokeAllByUserId(eq(10L), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("TC-F2.4.1-04a: activateAccountIfDisabled chỉ mở tài khoản DISABLED, không mở LOCKED")
    void activateAccountIfDisabledChiMoTaiKhoanDisabledKhongMoLocked() {
        User disabledUser = User.builder()
            .email("disabled@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.DISABLED)
            .build();
        when(userRepository.findById(11L)).thenReturn(Optional.of(disabledUser));

        accountService.activateAccountIfDisabled(11L);
        assertThat(disabledUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).saveAndFlush(disabledUser);

        User lockedUser = User.builder()
            .email("locked@example.com")
            .role(UserRole.MEMBER)
            .status(UserStatus.LOCKED)
            .build();
        when(userRepository.findById(12L)).thenReturn(Optional.of(lockedUser));

        accountService.activateAccountIfDisabled(12L);
        assertThat(lockedUser.getStatus()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    @DisplayName("TC-F2.2.1-01c: tạo tài khoản MEMBER với email chữ thường, mật khẩu ngẫu nhiên đã băm BCrypt")
    void createMemberAccountChuanHoaEmailVaBamMatKhauNgauNhien() {
        when(userRepository.existsByEmail("new.member@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$randomBcryptHash");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 88L);
            return u;
        });

        Long userId = accountService.createMemberAccount("  New.Member@Example.COM ");

        assertThat(userId).isEqualTo(88L);

        ArgumentCaptor<String> rawPassCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(rawPassCaptor.capture());
        assertThat(rawPassCaptor.getValue()).isNotBlank().hasSizeGreaterThanOrEqualTo(32);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new.member@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$randomBcryptHash");
        assertThat(saved.getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getFailedLoginCount()).isZero();
    }

    @Test
    @DisplayName("TC-F2.2.1-04b: trùng email (kể cả ràng buộc uq_users_email khi đồng thời) trả 409")
    void createMemberAccountTrungEmailTra409() {
        when(userRepository.existsByEmail("used@example.com")).thenReturn(true);

        assertThatThrownBy(() -> accountService.createMemberAccount("USED@Example.com"))
            .isInstanceOf(ConflictException.class)
            .hasMessage(AccountService.EMAIL_ALREADY_USED_MESSAGE);
        verify(userRepository, never()).saveAndFlush(any());

        // Trường hợp đồng thời: qua được existsByEmail nhưng bị uq_users_email chặn ở DB
        when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(
            new DataIntegrityViolationException("lỗi",
                new RuntimeException("duplicate key value violates unique constraint \"uq_users_email\"")));

        assertThatThrownBy(() -> accountService.createMemberAccount("race@example.com"))
            .isInstanceOf(ConflictException.class)
            .hasMessage(AccountService.EMAIL_ALREADY_USED_MESSAGE);
    }
}
