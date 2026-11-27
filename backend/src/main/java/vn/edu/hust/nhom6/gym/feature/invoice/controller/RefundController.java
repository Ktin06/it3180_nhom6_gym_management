package vn.edu.hust.nhom6.gym.feature.invoice.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.RefundRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.RefundResultDto;
import vn.edu.hust.nhom6.gym.feature.invoice.service.RefundService;

/**
 * Hoàn tiền hóa đơn (UC-F5.5, S2-38.1). Tách khỏi {@code InvoiceController} vì đây là thao tác tiền có kiểm soát riêng.
 *
 * <p>Chỉ đòi đã đăng nhập ở đây; vai trò {@code BRANCH_MANAGER} xét trong {@link RefundService} để trả đúng câu 403
 * của SRS (E3) và xét trước lý do (body không gắn {@code @Valid}, service tự kiểm tra sau bước vai trò).
 */
@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "F5 – Hóa đơn")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refunds;

    @PostMapping("/{id}/refund")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hoàn tiền hóa đơn PAID bán nhầm: hóa đơn REFUNDED, gói CANCELLED, phiếu thu CANCELLED, "
            + "ghi audit_logs (UC-F5.5)",
        description = "Chỉ quản lý chi nhánh của hóa đơn. Chỉ hoàn khi mọi gói của hóa đơn chưa dùng (BR-F5-14): gói "
            + "ACTIVE/UPCOMING, chưa check-in, chưa có buổi PT (kể cả đã hủy), gói TIME thì hội viên không còn chỗ lớp "
            + "BOOKED chưa diễn ra. Không đổi gói khác, không hủy buổi PT hay chỗ đặt lớp.")
    @ApiResponse(responseCode = "200", description = "Đã hoàn tiền")
    @ApiResponse(responseCode = "400", description = "Thiếu lý do, lý do trắng hoặc quá 500 ký tự")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải quản lý chi nhánh")
    @ApiResponse(responseCode = "404", description = "Hóa đơn không tồn tại hoặc thuộc chi nhánh khác")
    @ApiResponse(responseCode = "409", description = "Hóa đơn vừa được cập nhật bởi thao tác khác")
    @ApiResponse(responseCode = "422", description = "Hóa đơn không PAID, hoặc gói của hóa đơn đã được dùng")
    public RefundResultDto refund(Authentication auth, HttpServletRequest http,
            @PathVariable @Positive(message = "ID hóa đơn phải lớn hơn 0.") Long id,
            @RequestBody(required = false) RefundRequest request) {
        return refunds.refund(CurrentUser.from(auth), id, request, http.getRemoteAddr());
    }
}
