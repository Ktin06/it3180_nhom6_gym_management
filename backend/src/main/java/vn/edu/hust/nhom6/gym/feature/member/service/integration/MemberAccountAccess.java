package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import java.util.Collection;
import java.util.Map;

/**
 * Hợp đồng F2 cho adapter gọi service F1 hiện có; adapter thuộc F2, không sửa F1.
 * Năng lực chưa có ở F1 là phụ thuộc cần ghi nhận, không tự bổ sung triển khai F1.
 * Thao tác ghi tham gia giao dịch hiện tại, cùng datasource, không REQUIRES_NEW.
 */
public interface MemberAccountAccess {
    Map<Long, String> emails(Collection<Long> userIds);
    /** Đổi tài khoản thành DISABLED và thu hồi mọi refresh token. */
    void disable(Long userId);
    /** Chỉ mở lại tài khoản DISABLED; không mở LOCKED. */
    void activateIfDisabled(Long userId);
}
