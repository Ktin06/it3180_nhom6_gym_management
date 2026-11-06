package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.LocalDate;

/**
 * Giao tiếp sang chức năng F2 (Hồ sơ hội viên) để tạo hồ sơ khi người dùng tự đăng ký (F1, ADR 0007).
 */
public interface MemberService {

    /**
     * Tạo hồ sơ hội viên gắn với tài khoản vừa đăng ký.
     *
     * @param userId id của tài khoản {@code users} vừa tạo
     * @param fullName họ và tên hội viên
     * @param phone số điện thoại đã chuẩn hóa (10 chữ số bắt đầu bằng 0)
     * @param dob ngày sinh (tùy chọn)
     * @param gender giới tính (tùy chọn)
     */
    void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender);
}
