package vn.edu.hust.nhom6.gym.feature.member.service;

import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberAccountAccess;
import vn.edu.hust.nhom6.gym.feature.member.service.integration.MemberBranchAccess;

/** Chưa có tích hợp thì báo lỗi rõ, không giả lập thành công hay tự ghi bảng F1. */
@Component
@RequiredArgsConstructor
public class MemberDependencies {
    private final ObjectProvider<MemberAccountAccess> accountAccess;
    private final ObjectProvider<MemberBranchAccess> branchAccess;

    public Map<Long, String> emails(Collection<Long> userIds) {
        return userIds.isEmpty() ? Map.of() : accounts().emails(userIds);
    }
    public void disable(Long userId) { accounts().disable(userId); }
    public void activateIfDisabled(Long userId) { accounts().activateIfDisabled(userId); }
    public Long defaultBranchId() { return requireBranchId(branches().defaultBranchId()); }
    public Long staffBranchId(Long userId) { return requireBranchId(branches().staffBranchId(userId)); }

    private Long requireBranchId(Long id) {
        if (id == null || id <= 0) { throw new AccessDeniedException("Chưa có chi nhánh hợp lệ."); }
        return id;
    }

    private MemberAccountAccess accounts() {
        var service = accountAccess.getIfAvailable();
        if (service == null) { throw new BusinessRuleException("Chưa kết nối dịch vụ tài khoản F1 cho hội viên."); }
        return service;
    }
    private MemberBranchAccess branches() {
        var service = branchAccess.getIfAvailable();
        if (service == null) { throw new BusinessRuleException("Chưa kết nối dịch vụ chi nhánh và nhân sự cho hội viên."); }
        return service;
    }
}
