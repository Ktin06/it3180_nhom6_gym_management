package vn.edu.hust.nhom6.gym.feature.member.service;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;

/** Đọc Authentication đã xác thực: name là userId (JWT sub), authority ROLE_... . F2 không giải mã JWT. */
public record MemberActor(Long userId, Role role) {
    public enum Role { SUPER_ADMIN, BRANCH_MANAGER, RECEPTIONIST, TRAINER, MEMBER }

    public static MemberActor from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) { throw invalid(); }
        try {
            long userId = Long.parseLong(authentication.getName());
            if (userId <= 0) { throw invalid(); }
            Set<String> authorities = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet());
            Role selected = null;
            for (Role role : Role.values()) {
                if (authorities.contains("ROLE_" + role.name())) {
                    if (selected != null) { throw invalid(); }
                    selected = role;
                }
            }
            if (selected == null) { throw invalid(); }
            return new MemberActor(userId, selected);
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private static AuthenticationCredentialsNotFoundException invalid() {
        return new AuthenticationCredentialsNotFoundException("Thông tin xác thực không hợp lệ.");
    }
}
