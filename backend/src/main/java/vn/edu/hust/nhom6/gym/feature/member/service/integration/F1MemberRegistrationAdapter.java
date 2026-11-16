package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import vn.edu.hust.nhom6.gym.feature.auth.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberRegistrationService;

/**
 * Triển khai trực tiếp hợp đồng tạo hồ sơ của F1, chuyển tiếp sang service F2.
 * Bean thường được Spring chọn thay cho DefaultMemberService có @Fallback của F1.
 */
@Component("f2MemberRegistrationAdapter")
@RequiredArgsConstructor
public class F1MemberRegistrationAdapter implements MemberService {
    private final MemberRegistrationService registrations;

    @Override
    public void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender) {
        registrations.registerMemberProfile(userId, fullName, phone, dob, gender);
    }
}
