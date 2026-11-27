package vn.edu.hust.nhom6.gym.feature.invoice.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoicePdfDocument;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.VietQrPaymentDto;
import vn.edu.hust.nhom6.gym.feature.invoice.service.InvoiceDocumentService;

/**
 * Controller riêng cho chứng từ hóa đơn: sinh mã VietQR (UC-F5.1) và xuất hóa đơn PDF (UC-F5.4).
 */
@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "F5 – Hóa đơn")
@RequiredArgsConstructor
public class InvoiceDocumentController {
    private final InvoiceDocumentService documents;

    @GetMapping("/{id}/vietqr")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'BRANCH_MANAGER')")
    @Operation(summary = "Sinh thông tin và URL ảnh VietQR động cho hóa đơn chờ thanh toán (UC-F5.1)",
        description = "Chỉ áp dụng cho hóa đơn PENDING trong chi nhánh của nhân viên. Số tiền lấy từ invoices.total "
            + "(BR-F5-01), nội dung chuyển khoản '{branch_code} {invoice_code}' (BR-F5-02). Tài khoản ngân hàng đọc "
            + "từ system_configs (chi nhánh ghi đè toàn chuỗi); không gọi mạng ra ngoài.")
    @ApiResponse(responseCode = "200", description = "Thông tin và URL ảnh VietQR")
    @ApiResponse(responseCode = "400", description = "ID hóa đơn không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân hay quản lý chi nhánh")
    @ApiResponse(responseCode = "404", description = "Hóa đơn không tồn tại hoặc thuộc chi nhánh khác")
    @ApiResponse(responseCode = "422", description = "Hóa đơn không ở trạng thái PENDING, hóa đơn MEMBER_PORTAL quá hạn hoặc chi nhánh chưa cấu hình tài khoản ngân hàng")
    public VietQrPaymentDto vietQr(Authentication auth,
            @PathVariable @Positive(message = "ID hóa đơn phải lớn hơn 0.") Long id) {
        return documents.generateVietQr(CurrentUser.from(auth), id);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'BRANCH_MANAGER', 'MEMBER')")
    @Operation(summary = "Xuất hóa đơn thanh toán định dạng PDF (UC-F5.4)",
        description = "Chỉ áp dụng cho hóa đơn PAID hoặc REFUNDED. Nhân viên chỉ xuất hóa đơn chi nhánh mình; hội viên "
            + "chỉ xuất hóa đơn của chính mình (ngoài phạm vi trả 404). Mô tả và đơn giá lấy từ invoice_items (BR-F5-09).")
    @ApiResponse(responseCode = "200", description = "Luồng nhị phân file PDF (application/pdf)")
    @ApiResponse(responseCode = "400", description = "ID hóa đơn không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân, quản lý chi nhánh hoặc hội viên")
    @ApiResponse(responseCode = "404", description = "Hóa đơn không tồn tại, ngoài chi nhánh hoặc của hội viên khác")
    @ApiResponse(responseCode = "422", description = "Hóa đơn đang PENDING hoặc CANCELLED")
    public ResponseEntity<byte[]> pdf(Authentication auth,
            @PathVariable @Positive(message = "ID hóa đơn phải lớn hơn 0.") Long id) {
        InvoicePdfDocument doc = documents.generatePdf(CurrentUser.from(auth), id);
        ContentDisposition disposition = ContentDisposition.inline()
            .filename(doc.filename())
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentLength(doc.content().length)
            .body(doc.content());
    }
}
