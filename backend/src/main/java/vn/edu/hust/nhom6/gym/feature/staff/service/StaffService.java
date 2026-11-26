package vn.edu.hust.nhom6.gym.feature.staff.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.staff.repository.StaffLookupRepository;

/**
 * Chi nhánh làm việc của nhân viên (F7). Mọi chức năng cần "nhân viên này thuộc chi nhánh nào" gọi lớp này,
 * không tự truy vấn bảng {@code staff}.
 *
 * <p>Chủ chuỗi ({@code SUPER_ADMIN}) <b>không có dòng {@code staff}</b>, gọi {@link #requireBranchId(Long)} sẽ bị 403.
 * Chỗ nào chủ chuỗi được xem mọi chi nhánh thì kiểm tra trước, không gọi lớp này:
 *
 * <pre>{@code
 * Long branchId = me.isOwner() ? null : staffService.requireBranchId(me.userId()); // null = mọi chi nhánh
 * }</pre>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffService {
    private final StaffLookupRepository staff;
    private final BranchLookupService branches;

    /**
     * Chi nhánh của nhân viên đang gọi API.
     *
     * @return {@code branch_id} của dòng {@code staff} ACTIVE gắn với tài khoản, chi nhánh đó cũng ACTIVE
     * @throws AccessDeniedException (403) khi không có dòng staff ACTIVE (kể cả chủ chuỗi) hoặc chi nhánh đã ngừng
     */
    public Long requireBranchId(Long userId) {
        if (userId == null || userId <= 0) {
            throw missingAssignment();
        }
        Long branchId = staff.findActiveBranchIdByUserId(userId).orElseThrow(StaffService::missingAssignment);
        if (!branches.isActive(branchId)) {
            throw new AccessDeniedException("Chi nhánh làm việc không tồn tại hoặc đã ngừng hoạt động.");
        }
        return branchId;
    }

    /** Họ tên nhân viên theo {@code users.id}, ví dụ người thu tiền trên phiếu thu; rỗng nếu không có dòng staff. */
    public Optional<String> findFullName(Long userId) {
        return staff.findFullNameByUserId(userId);
    }

    private static AccessDeniedException missingAssignment() {
        return new AccessDeniedException("Nhân viên chưa có chi nhánh làm việc hợp lệ.");
    }
}
