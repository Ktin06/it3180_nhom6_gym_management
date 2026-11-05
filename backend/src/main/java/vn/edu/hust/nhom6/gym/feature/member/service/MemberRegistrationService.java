package vn.edu.hust.nhom6.gym.feature.member.service;

import java.time.LocalDate;

/** Cổng tạo hồ sơ F2; F1MemberRegistrationAdapter nối interface F1 hiện có vào service này. */
public interface MemberRegistrationService {
    void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender);
}
