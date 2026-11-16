package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.util.Collection;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import vn.edu.hust.nhom6.gym.feature.auth.service.AuthService;

/**
 * F2 gọi trực tiếp service tài khoản F1 theo ADR 0007, không truy cập repository F1.
 * AuthService do Spring quản lý tham gia giao dịch hiện tại của F2.
 */
@Component("f1MemberAccountAdapter")
@RequiredArgsConstructor
public class F1MemberAccountAdapter implements MemberAccountAccess {
    private final AuthService authService;

    @Override
    public Map<Long, String> emails(Collection<Long> userIds) {
        return authService.getEmailsByUserIds(userIds);
    }

    @Override
    public void disable(Long userId) { authService.disableAccount(userId); }

    @Override
    public void activateIfDisabled(Long userId) { authService.activateAccountIfDisabled(userId); }
}
