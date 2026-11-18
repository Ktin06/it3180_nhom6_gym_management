package vn.edu.hust.nhom6.gym.feature.packagecatalog.service;

import java.util.Comparator;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PublicPackageDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.GymPackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.mapper.PackageMapper;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.repository.PackageRepository;

/**
 * Bảng giá công khai cho khách chưa đăng nhập (UC-F3.1, US-F3.1.2).
 *
 * <p>Tách khỏi {@link PackageCatalogService} vì API nội bộ gắn với vai trò và chi nhánh của nhân viên,
 * còn API này không có người dùng đăng nhập.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicPackageService {
    /** TIME, SESSION, PT theo thứ tự khai báo của enum; cùng loại thì rẻ trước. */
    private static final Comparator<GymPackage> DISPLAY_ORDER = Comparator
        .comparing(GymPackage::getType)
        .thenComparing(GymPackage::getPrice)
        .thenComparing(GymPackage::getName)
        .thenComparing(GymPackage::getId);

    private final PackageRepository packages;
    private final BranchLookupService branches;

    /**
     * Gói ACTIVE của chi nhánh mặc định và gói toàn chuỗi (BR-F3-01). Từ v1 đến v3 hệ thống chạy một
     * chi nhánh mặc định, nên khách không chọn chi nhánh (AC-F3.1.2-01, AC-F3.1.2-02).
     */
    public List<PublicPackageDto> listActive() {
        return packages.findByStatusInBranchOrGlobal(PackageStatus.ACTIVE, branches.defaultBranchId()).stream()
            .sorted(DISPLAY_ORDER)
            .map(PackageMapper::publicView)
            .toList();
    }
}
