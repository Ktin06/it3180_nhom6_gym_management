package vn.edu.hust.nhom6.gym.feature.memberportal.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.MyCheckinDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.MyInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.MyMembershipDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.service.MemberPortalQueryService;

/**
 * Các API chỉ đọc của cổng hội viên (F7 UC-F7.1–UC-F7.3). Tách riêng khỏi {@link PurchaseRequestController} (S2-14.1).
 * Mọi endpoint chỉ dành cho vai trò {@code MEMBER} và luôn lấy hội viên từ token, không nhận {@code memberId} (BR-F7.1).
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "F7 – Cổng hội viên")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class MemberPortalController {
    private final MemberPortalQueryService queries;

    @GetMapping("/memberships")
    @Operation(summary = "Xem danh sách gói tập của tôi (UC-F7.1)",
        description = "Hội viên lấy từ token. Trả toàn bộ gói của mình ở mọi trạng thái kèm tên gói, loại, mã trạng "
            + "thái, ngày hiệu lực và số buổi còn lại; chưa có gói trả danh sách rỗng.")
    @ApiResponse(responseCode = "200", description = "Danh sách gói của hội viên, có thể rỗng")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên, hoặc tài khoản chưa gắn hồ sơ")
    public List<MyMembershipDto> memberships(Authentication auth) {
        return queries.myMemberships(CurrentUser.from(auth));
    }

    @GetMapping("/checkins")
    @Operation(summary = "Xem lịch sử check-in của tôi, phân trang, mới nhất trước (UC-F7.2)",
        description = "Hội viên lấy từ token. Trả PageResponse gồm chi nhánh, ngày Việt Nam (checkinDate), giờ vào, "
            + "giờ ra và gói đã dùng.")
    @ApiResponse(responseCode = "200", description = "Trang lịch sử check-in, có thể rỗng")
    @ApiResponse(responseCode = "400", description = "Tham số phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên, hoặc tài khoản chưa gắn hồ sơ")
    public PageResponse<MyCheckinDto> checkins(Authentication auth,
            @Parameter(description = "Trang từ 0") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Trang phải từ 0.") int page,
            @Parameter(description = "Số bản ghi mỗi trang, từ 1 đến 100") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100.")
            @Max(value = 100, message = "Kích thước trang phải từ 1 đến 100.") int size) {
        return queries.myCheckins(CurrentUser.from(auth), page, size);
    }

    @GetMapping("/invoices")
    @Operation(summary = "Xem danh sách hóa đơn của tôi, phân trang, mới nhất trước (UC-F7.3)",
        description = "Hội viên lấy từ token. Hóa đơn chờ từ cổng hội viên (PENDING + MEMBER_PORTAL) có thêm hạn tự "
            + "hủy expiresAt = created_at + PENDING_INVOICE_TTL_DAYS đọc từ system_configs (BR-F7.4).")
    @ApiResponse(responseCode = "200", description = "Trang danh sách hóa đơn, có thể rỗng")
    @ApiResponse(responseCode = "400", description = "Tham số phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên, hoặc tài khoản chưa gắn hồ sơ")
    public PageResponse<MyInvoiceDto> invoices(Authentication auth,
            @Parameter(description = "Trang từ 0") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Trang phải từ 0.") int page,
            @Parameter(description = "Số bản ghi mỗi trang, từ 1 đến 100") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100.")
            @Max(value = 100, message = "Kích thước trang phải từ 1 đến 100.") int size) {
        return queries.myInvoices(CurrentUser.from(auth), page, size);
    }
}
