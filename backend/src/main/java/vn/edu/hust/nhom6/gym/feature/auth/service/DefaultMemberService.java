package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;

import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.util.PhoneUtils;

/**
 * Xử lý tạo hồ sơ hội viên mặc định cho F1 (F1 gọi F2 qua service theo ADR 0007).
 *
 * <p>Kiểm tra tính hợp lệ của số điện thoại và chặn tự đăng ký nếu số điện thoại đã có hồ sơ (409 Conflict).
 */
@Service
@Fallback
public class DefaultMemberService implements MemberService {

    private final Set<String> registeredPhones = ConcurrentHashMap.newKeySet();

    @Override
    public void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender) {
        String normalized = PhoneUtils.normalizePhone(phone);
        if (!registeredPhones.add(normalized)) {
            throw new ConflictException("Số điện thoại đã có hồ sơ hội viên. Vui lòng liên hệ quầy lễ tân để được hỗ trợ.");
        }
    }
}
