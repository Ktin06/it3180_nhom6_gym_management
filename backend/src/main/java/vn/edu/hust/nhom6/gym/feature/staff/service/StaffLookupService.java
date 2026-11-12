package vn.edu.hust.nhom6.gym.feature.staff.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.feature.staff.repository.StaffLookupRepository;

/** Cung cấp chi nhánh từ hồ sơ nhân viên đang hoạt động, không tự gán CN01. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffLookupService {
    private final StaffLookupRepository staff;

    public Long activeBranchIdForUser(Long userId) {
        if (userId == null || userId <= 0) { throw missingAssignment(); }
        return staff.findActiveBranchIdByUserId(userId).orElseThrow(StaffLookupService::missingAssignment);
    }

    private static AccessDeniedException missingAssignment() {
        return new AccessDeniedException("Nhân viên chưa có chi nhánh làm việc hợp lệ.");
    }
}
