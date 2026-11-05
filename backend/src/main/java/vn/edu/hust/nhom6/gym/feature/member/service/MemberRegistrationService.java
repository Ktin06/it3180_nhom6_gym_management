package vn.edu.hust.nhom6.gym.feature.member.service;

import java.time.LocalDate;

/** Cổng F2 cung cấp để người phụ trách F1 nối vào luồng đăng ký của họ. */
public interface MemberRegistrationService {
    void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender);
}
