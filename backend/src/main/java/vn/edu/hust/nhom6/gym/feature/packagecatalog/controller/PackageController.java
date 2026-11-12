package vn.edu.hust.nhom6.gym.feature.packagecatalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageDeactivationDto;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.UpdatePackageRequest;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageSummaryDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.CreatePackageRequest;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageDetailDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.service.PackageCatalogService;

@RestController
@RequestMapping("/api/v1/packages")
@Tag(name = "F3 – Danh mục gói tập")
@RequiredArgsConstructor
public class PackageController {
    private final PackageCatalogService packages;

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Ngừng bán gói của chi nhánh",
        description = "Không yêu cầu request body; tương thích với client gửi {}. Nội dung body không được sử dụng. "
            + "ACTIVE chuyển INACTIVE, không xóa bản ghi và không đổi membership/hóa đơn cũ. "
            + "Gọi lại trả 422. Gói khác chi nhánh/toàn chuỗi trả 403; ID không tồn tại trả 404.")
    @ApiResponse(responseCode = "200", description = "id, code, status INACTIVE và updatedAt UTC")
    @ApiResponse(responseCode = "400", description = "ID không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Sai vai trò, thiếu staff ACTIVE hoặc gói khác chi nhánh/toàn chuỗi")
    @ApiResponse(responseCode = "404", description = "ID gói không tồn tại")
    @ApiResponse(responseCode = "422", description = "Gói đã ngừng bán")
    public PackageDeactivationDto deactivate(Authentication auth,
            @PathVariable @Positive(message = "ID gói phải lớn hơn 0.") Long id) {
        return packages.deactivate(auth, id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Sửa từng phần thông tin gói của chi nhánh",
        description = "Không gửi trường thì giữ nguyên; description:null xóa mô tả. "
            + "Không đổi code/type/branchId/status. Trường bắt buộc null trả 400. "
            + "Gói khác chi nhánh/toàn chuỗi trả 403; ID không tồn tại trả 404. Không đổi lịch sử đã bán.")
    @ApiResponse(responseCode = "200", description = "Chi tiết gói sau cập nhật, kể cả body rỗng không thay đổi")
    @ApiResponse(responseCode = "400", description = "Dữ liệu hoặc ID không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Sai vai trò, thiếu staff ACTIVE hoặc gói khác chi nhánh/toàn chuỗi")
    @ApiResponse(responseCode = "404", description = "ID gói không tồn tại")
    public PackageDetailDto update(Authentication auth,
            @PathVariable @Positive(message = "ID gói phải lớn hơn 0.") Long id,
            @Valid @RequestBody UpdatePackageRequest request) {
        return packages.update(auth, id, request);
    }

    @PostMapping
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Thêm gói tập vào danh mục của chi nhánh",
        description = "Chi nhánh lấy từ staff ACTIVE của quản lý; trạng thái mặc định ACTIVE. "
            + "TIME không có số buổi; SESSION/PT có số buổi dương. Mã duy nhất toàn hệ thống. "
            + "Chỉ nhận các trường DTO; từ chối branchId/status/id và trường lạ.")
    @ApiResponse(responseCode = "201", description = "Tạo gói thành công, trả chi tiết và Location")
    @ApiResponse(responseCode = "400", description = "JSON hoặc dữ liệu không hợp lệ, có errors theo trường")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải quản lý hoặc thiếu chi nhánh ACTIVE")
    @ApiResponse(responseCode = "409", description = "Mã gói đã tồn tại, kể cả gói INACTIVE")
    public ResponseEntity<PackageDetailDto> create(Authentication auth, @Valid @RequestBody CreatePackageRequest request) {
        var result = packages.create(auth, request);
        return ResponseEntity.created(URI.create("/api/v1/packages/" + result.id())).body(result);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_MANAGER','RECEPTIONIST')")
    @Operation(summary = "Xem danh mục gói tập trong phạm vi chi nhánh",
        description = "Mặc định ACTIVE; sắp xếp type, name, id. Chủ chuỗi xem toàn bộ; quản lý/lễ tân "
            + "xem gói chi nhánh mình và toàn chuỗi. Không nhận bộ lọc chi nhánh từ client.")
    @ApiResponse(responseCode = "200", description = "Danh mục phân trang, có thể rỗng")
    @ApiResponse(responseCode = "400", description = "Bộ lọc hoặc phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Sai vai trò hoặc chưa có chi nhánh ACTIVE")
    public PageResponse<PackageSummaryDto> list(Authentication auth,
            @RequestParam(required = false) PackageType type,
            @RequestParam(defaultValue = "ACTIVE") PackageStatus status,
            @Parameter(description = "Trang từ 0") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Trang phải từ 0.") int page,
            @Parameter(description = "Số gói mỗi trang, từ 1 đến 100") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100.")
            @Max(value = 100, message = "Kích thước trang phải từ 1 đến 100.") int size) {
        return packages.list(auth, type, status, page, size);
    }
}
