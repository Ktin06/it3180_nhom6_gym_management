package vn.edu.hust.nhom6.gym.feature.packagecatalog.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PublicPackageDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.GymPackage;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
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
    /** Thứ tự loại gói trên bảng giá, ghi rõ để không phụ thuộc thứ tự khai báo của enum. */
    private static final Map<PackageType, Integer> TYPE_ORDER =
        Map.of(PackageType.TIME, 0, PackageType.SESSION, 1, PackageType.PT, 2);

    /** TIME, SESSION, PT; cùng loại thì rẻ trước, rồi theo tên. */
    private static final Comparator<GymPackage> DISPLAY_ORDER = Comparator
        .comparing((GymPackage p) -> TYPE_ORDER.get(p.getType()))
        .thenComparing(GymPackage::getPrice)
        .thenComparing(GymPackage::getName)
        .thenComparing(GymPackage::getId);

    private final PackageRepository packages;
    private final BranchLookupService branches;

    /**
     * Gói ACTIVE của chi nhánh mặc định và gói toàn chuỗi (BR-F3-01). Từ v1 đến v3 hệ thống chạy một
     * chi nhánh mặc định, nên khách không chọn chi nhánh (AC-F3.1.2-01, AC-F3.1.2-02).
     * Chi nhánh mặc định đã ngừng hoạt động thì trả danh sách rỗng, không báo lỗi (AC-F3.1.2-06).
     */
    public List<PublicPackageDto> listActive() {
        return branches.findActiveDefaultBranchId()
            .map(branchId -> packages.search(false, branchId, null, PackageStatus.ACTIVE, Pageable.unpaged())
                .stream()
                .sorted(DISPLAY_ORDER)
                .map(PackageMapper::publicView)
                .toList())
            .orElse(List.of());
    }
}
