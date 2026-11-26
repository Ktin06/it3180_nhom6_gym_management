package vn.edu.hust.nhom6.gym.feature.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.hust.nhom6.gym.feature.auth.entity.User;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserStatus;
import vn.edu.hust.nhom6.gym.feature.auth.repository.RefreshTokenRepository;
import vn.edu.hust.nhom6.gym.feature.auth.repository.UserRepository;
import vn.edu.hust.nhom6.gym.feature.auth.service.AccountService;

/** Hai kiểm thử dưới chuyển nguyên từ AuthServiceTest khi tách AccountService (nền tảng N0.1). */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(userRepository, refreshTokenRepository);
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
}
