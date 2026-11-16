package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffLookupService;

/** F2 gọi dịch vụ chi nhánh/nhân viên qua service theo ADR 0007, không gọi chéo repository. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBranchAdapter implements MemberBranchAccess {
    private final BranchLookupService branches;
    private final StaffLookupService staff;

    @Override
    public Long defaultBranchId() { return branches.defaultBranchId(); }

    @Override
    public Long staffBranchId(Long userId) {
        Long branchId = staff.activeBranchIdForUser(userId);
        if (!branches.isActive(branchId)) {
            throw new AccessDeniedException("Chi nhánh làm việc không tồn tại hoặc đã ngừng hoạt động.");
        }
        return branchId;
    }
}
