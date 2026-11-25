package vn.edu.hust.nhom6.gym.feature.memberportal.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PortalInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.PurchaseRequest;
import vn.edu.hust.nhom6.gym.feature.memberportal.dto.RenewalRequest;
import vn.edu.hust.nhom6.gym.feature.memberportal.service.PurchaseRequestService;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "F7 – Cổng hội viên")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class PurchaseRequestController {
    private final PurchaseRequestService requests;

    @PostMapping("/purchase-requests")
    @Operation(summary = "Hội viên gửi yêu cầu mua gói: tạo hóa đơn chờ và gói PENDING, trả tại quầy (UC-F7.4)",
        description = "Hội viên lấy từ token. Giá tính ở server. Hóa đơn tự hủy sau PENDING_INVOICE_TTL_DAYS ngày "
            + "(expiresAt).")
    @ApiResponse(responseCode = "201", description = "Đã tạo hóa đơn chờ")
    @ApiResponse(responseCode = "400", description = "Thiếu hoặc sai packageId")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên, hoặc tài khoản chưa gắn hồ sơ")
    @ApiResponse(responseCode = "404", description = "Gói không tồn tại")
    @ApiResponse(responseCode = "409", description = "Đã có hóa đơn chờ cho gói này")
    @ApiResponse(responseCode = "422", description = "Gói ngừng bán hoặc không áp dụng tại chi nhánh, hội viên ngừng "
        + "hoạt động, hoặc đã có gói cùng loại đang chờ thanh toán/kích hoạt")
    public ResponseEntity<PortalInvoiceDto> purchase(Authentication auth,
            @Valid @RequestBody PurchaseRequest request) {
        return created(requests.purchase(CurrentUser.from(auth), request.packageId()));
    }

    @PostMapping("/renewal-requests")
    @Operation(summary = "Hội viên gửi yêu cầu gia hạn một gói của mình: tạo hóa đơn chờ và gói mới PENDING (UC-F7.5)",
        description = "Gói mới cùng gói danh mục với gói cũ; gói cũ không đổi. Ngày bắt đầu tính lại khi thu tiền.")
    @ApiResponse(responseCode = "201", description = "Đã tạo hóa đơn chờ")
    @ApiResponse(responseCode = "400", description = "Thiếu hoặc sai membershipId")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên, hoặc gói không thuộc hội viên này")
    @ApiResponse(responseCode = "404", description = "Gói của hội viên không tồn tại")
    @ApiResponse(responseCode = "409", description = "Đã có hóa đơn chờ cho gói này")
    @ApiResponse(responseCode = "422", description = "Gói đã ngừng bán, hoặc đã có gói cùng loại đang chờ")
    public ResponseEntity<PortalInvoiceDto> renew(Authentication auth, @Valid @RequestBody RenewalRequest request) {
        return created(requests.renew(CurrentUser.from(auth), request.membershipId()));
    }

    private static ResponseEntity<PortalInvoiceDto> created(PortalInvoiceDto result) {
        return ResponseEntity.created(URI.create("/api/v1/me/invoices/" + result.invoiceId())).body(result);
    }
}
