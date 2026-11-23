package vn.edu.hust.nhom6.gym.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import vn.edu.hust.nhom6.gym.common.exception.UnauthorizedException;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;

/**
 * Người đang gọi API: id tài khoản ({@code users.id}, claim {@code sub} của JWT) và vai trò.
 *
 * <p>Mọi controller lấy người gọi bằng một cách duy nhất: nhận tham số {@code Authentication auth} rồi gọi
 * {@link #from(Authentication)}, sau đó truyền {@code CurrentUser} xuống service. Service không đọc JWT hay
 * {@code SecurityContextHolder}, nên kiểm thử đơn vị chỉ cần {@code new CurrentUser(10L, UserRole.RECEPTIONIST)}.
 *
 * <pre>{@code
 * @GetMapping("/{id}")
 * @PreAuthorize("hasAnyRole('BRANCH_MANAGER','RECEPTIONIST')")
 * public MemberDetailResponse detail(Authentication auth, @PathVariable Long id) {
 *     return members.detail(CurrentUser.from(auth), id);
 * }
 * }</pre>
 *
 * @param userId id tài khoản, luôn lớn hơn 0
 * @param role vai trò duy nhất của tài khoản
 */
public record CurrentUser(Long userId, UserRole role) {

    private static final String INVALID = "Thông tin xác thực không hợp lệ.";
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Đọc người gọi từ {@code Authentication} mà Spring Security đã xác thực.
     *
     * <p>{@code getName()} là claim {@code sub} (id tài khoản); vai trò lấy từ quyền {@code ROLE_<vai trò>}
     * do {@code SecurityConfig} tạo từ claim {@code role}. Không giải mã lại JWT ở đây.
     *
     * @throws UnauthorizedException (401) khi chưa xác thực, {@code sub} không phải số dương,
     *         hoặc không có đúng một vai trò hợp lệ
     */
    public static CurrentUser from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw invalid();
        }
        long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw invalid();
        }
        if (userId <= 0) {
            throw invalid();
        }
        UserRole selected = null;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            UserRole role = roleOf(authority.getAuthority());
            if (role == null) {
                continue;
            }
            if (selected != null && selected != role) {
                throw invalid();
            }
            selected = role;
        }
        if (selected == null) {
            throw invalid();
        }
        return new CurrentUser(userId, selected);
    }

    /** {@code true} nếu người gọi có đúng vai trò này. */
    public boolean is(UserRole expected) {
        return role == expected;
    }

    /** Chủ chuỗi ({@code SUPER_ADMIN}) xem được mọi chi nhánh và không có dòng {@code staff}. */
    public boolean isOwner() {
        return role == UserRole.SUPER_ADMIN;
    }

    private static UserRole roleOf(String authority) {
        if (authority == null || !authority.startsWith(ROLE_PREFIX)) {
            return null;
        }
        try {
            return UserRole.valueOf(authority.substring(ROLE_PREFIX.length()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static UnauthorizedException invalid() {
        return new UnauthorizedException(INVALID);
    }
}
