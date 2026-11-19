package vn.edu.hust.nhom6.gym.feature.branch.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.feature.branch.repository.BranchLookupRepository;

/** Tra cứu tối thiểu phục vụ các chức năng khác trong giai đoạn một chi nhánh. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchLookupService {
    private static final String DEFAULT_BRANCH_CODE = "CN01";
    private final BranchLookupRepository branches;

    public Long defaultBranchId() {
        return findActiveDefaultBranchId()
            .orElseThrow(() -> new NotFoundException("Không tìm thấy chi nhánh mặc định CN01 đang hoạt động."));
    }

    /** Chi nhánh mặc định CN01 nếu đang hoạt động; rỗng nếu đã ngừng, để nơi gọi tự quyết cách xử lý. */
    public Optional<Long> findActiveDefaultBranchId() {
        return branches.findActiveIdByCode(DEFAULT_BRANCH_CODE);
    }

    public boolean isActive(Long branchId) {
        return branchId != null && branchId > 0 && branches.existsActiveById(branchId);
    }
}
