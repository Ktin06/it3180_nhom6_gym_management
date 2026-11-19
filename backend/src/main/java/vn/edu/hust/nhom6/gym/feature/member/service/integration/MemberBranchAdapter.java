package vn.edu.hust.nhom6.gym.feature.member.service.integration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

/** F2 gọi dịch vụ chi nhánh/nhân viên qua service theo ADR 0007, không gọi chéo repository. */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBranchAdapter implements MemberBranchAccess {
    private final BranchLookupService branches;
    private final StaffService staff;

    @Override
    public Long defaultBranchId() { return branches.defaultBranchId(); }

    @Override
    public Long staffBranchId(Long userId) { return staff.requireBranchId(userId); }
}
