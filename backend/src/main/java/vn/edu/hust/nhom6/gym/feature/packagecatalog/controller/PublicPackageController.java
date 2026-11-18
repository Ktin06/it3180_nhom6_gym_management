package vn.edu.hust.nhom6.gym.feature.packagecatalog.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PublicPackageDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.service.PublicPackageService;

/** Bảng giá công khai cho khách, không cần đăng nhập (US-F3.1.2). */
@RestController
@RequestMapping("/api/v1/public/packages")
@Tag(name = "F3 – Danh mục gói tập")
@RequiredArgsConstructor
public class PublicPackageController {
    private final PublicPackageService publicPackages;

    @GetMapping
    @Operation(summary = "Bảng giá công khai",
        description = "Không cần đăng nhập. Trả các gói ACTIVE của chi nhánh mặc định và gói toàn chuỗi, "
            + "sắp theo loại (TIME, SESSION, PT) rồi giá. Không phân trang; chỉ có các trường cần hiển thị.")
    @ApiResponse(responseCode = "200", description = "Danh sách gói đang bán, có thể rỗng")
    public List<PublicPackageDto> list() {
        return publicPackages.listActive();
    }
}
