package vn.edu.hust.nhom6.gym.feature.auth.entity;

/**
 * 5 vai trò có tài khoản đăng nhập trong hệ thống; khớp với ràng buộc {@code ck_users_role} của bảng {@code users}.
 *
 * <p>Khách không có tài khoản đăng nhập và chỉ truy cập các đường dẫn công khai.
 */
public enum UserRole {
    SUPER_ADMIN,
    BRANCH_MANAGER,
    RECEPTIONIST,
    TRAINER,
    MEMBER
}
