package vn.edu.hust.nhom6.gym.feature.invoice.controller;

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
import vn.edu.hust.nhom6.gym.feature.invoice.dto.CreateInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceService;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "F3 – Bán và gia hạn gói")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoices;

    @PostMapping
    @PreAuthorize("hasRole('RECEPTIONIST')")
    @Operation(summary = "Bán hoặc gia hạn gói tại quầy: tạo hóa đơn PENDING và gói PENDING (UC-F3.5)",
        description = "Giá và tổng tiền tính ở server; giá client gửi kèm bị bỏ qua. Ngày bắt đầu, kết thúc là dự kiến, "
            + "tính lại khi thu tiền. Hội viên ngoài chi nhánh trả 404.")
    @ApiResponse(responseCode = "201", description = "Hóa đơn và gói PENDING đã tạo")
    @ApiResponse(responseCode = "400", description = "Thiếu memberId, items, packageId hoặc dòng không phải PACKAGE")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân")
    @ApiResponse(responseCode = "404", description = "Hội viên hoặc gói không tồn tại")
    @ApiResponse(responseCode = "422", description = "Hội viên ngừng hoạt động, gói ngừng bán, gói chi nhánh khác, "
        + "hoặc đã có gói cùng loại đang chờ")
    public ResponseEntity<PackageInvoiceDto> create(Authentication auth,
            @Valid @RequestBody CreateInvoiceRequest request) {
        var result = invoices.createAtCounter(CurrentUser.from(auth), request);
        return ResponseEntity.created(URI.create("/api/v1/invoices/" + result.invoiceId())).body(result);
    }
}
