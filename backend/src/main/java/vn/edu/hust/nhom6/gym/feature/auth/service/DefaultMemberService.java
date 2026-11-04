package vn.edu.hust.nhom6.gym.feature.auth.service;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;

/**
 * Xử lý tạo hồ sơ hội viên mặc định cho F1 (F1 gọi F2 qua service theo ADR 0007).
 *
 * <p>Kiểm tra tính hợp lệ của số điện thoại và chặn tự đăng ký nếu số điện thoại đã có hồ sơ (409 Conflict).
 */
@Service
public class DefaultMemberService implements MemberService {

    private final Set<String> registeredPhones = ConcurrentHashMap.newKeySet();

    @Override
    public void registerMemberProfile(Long userId, String fullName, String phone, LocalDate dob, String gender) {
        if (phone == null || !phone.matches("^0\\d{9}$")) {
            throw new BusinessRuleException("Số điện thoại không hợp lệ. Phải gồm 10 chữ số bắt đầu bằng số 0.");
        }
        if (!registeredPhones.add(phone)) {
            throw new ConflictException("Số điện thoại đã có hồ sơ hội viên. Vui lòng liên hệ quầy lễ tân để được hỗ trợ.");
        }
    }
}
