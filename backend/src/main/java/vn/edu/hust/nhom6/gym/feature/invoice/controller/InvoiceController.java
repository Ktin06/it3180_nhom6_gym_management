package vn.edu.hust.nhom6.gym.feature.invoice.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.CancelledInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.CreateInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceDetailDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceBranchDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.InvoiceSummaryDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PackageInvoiceDto;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PayInvoiceRequest;
import vn.edu.hust.nhom6.gym.feature.invoice.dto.PaymentConfirmationDto;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceSource;
import vn.edu.hust.nhom6.gym.feature.invoice.entity.InvoiceStatus;
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

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'BRANCH_MANAGER')")
    @Operation(summary = "Xác nhận đã nhận tiền: hóa đơn PAID, ghi phiếu thu, kích hoạt gói ACTIVE hoặc UPCOMING (UC-F5.2)",
        description = "Số tiền là tổng hóa đơn do server tính, client không gửi. Ngày bắt đầu, kết thúc của gói tính "
            + "lại theo ngày thu tiền (giờ Việt Nam), giữ số ngày đã chốt lúc lập hóa đơn.")
    @ApiResponse(responseCode = "200", description = "Đã thu tiền và kích hoạt gói")
    @ApiResponse(responseCode = "400", description = "ID hóa đơn không hợp lệ; method thiếu hoặc khác CASH, BANK_TRANSFER; transactionRef quá dài")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân hay quản lý chi nhánh")
    @ApiResponse(responseCode = "404", description = "Hóa đơn không tồn tại hoặc thuộc chi nhánh khác")
    @ApiResponse(responseCode = "409", description = "Dữ liệu hóa đơn vừa được cập nhật bởi thao tác khác")
    @ApiResponse(responseCode = "422", description = "Hóa đơn không còn chờ thanh toán, hóa đơn MEMBER_PORTAL quá hạn, hội viên đã ngừng hoạt động, hoặc đã có gói cùng loại UPCOMING")
    public PaymentConfirmationDto pay(Authentication auth,
            @PathVariable @Positive(message = "ID hóa đơn phải lớn hơn 0.") Long id,
            @Valid @RequestBody PayInvoiceRequest request) {
        return invoices.pay(CurrentUser.from(auth), id, request);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'BRANCH_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Hủy hóa đơn chờ thanh toán, kèm gói PENDING của nó (UC-F5.3 luồng A1)")
    @ApiResponse(responseCode = "200", description = "Đã hủy")
    @ApiResponse(responseCode = "400", description = "ID hóa đơn không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải nhân viên quầy, quản lý hay chủ chuỗi")
    @ApiResponse(responseCode = "404", description = "Hóa đơn không tồn tại hoặc thuộc chi nhánh khác")
    @ApiResponse(responseCode = "409", description = "Người khác vừa thu tiền hoặc hủy hóa đơn này")
    @ApiResponse(responseCode = "422", description = "Hóa đơn không ở trạng thái chờ thanh toán")
    public CancelledInvoiceDto cancel(Authentication auth,
            @PathVariable @Positive(message = "ID hóa đơn phải lớn hơn 0.") Long id) {
        return invoices.cancel(CurrentUser.from(auth), id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'BRANCH_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Tra cứu danh sách hóa đơn phân trang, mới nhất trước (UC-F5.3)",
        description = "Lễ tân và quản lý chỉ thấy chi nhánh mình (branchId truyền vào bị bỏ qua); chủ chuỗi thấy toàn "
            + "chuỗi và lọc được theo branchId. Khoảng ngày fromDate..toDate tính theo ngày Việt Nam nửa mở. "
            + "Dùng LEFT JOIN members nên hóa đơn khách lẻ vẫn hiện.")
    @ApiResponse(responseCode = "200", description = "Danh sách hóa đơn phân trang")
    @ApiResponse(responseCode = "400", description = "Tham số lọc hoặc phân trang không hợp lệ (gồm keyword vượt quá 100 ký tự)")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân, quản lý chi nhánh hay chủ chuỗi")
    public PageResponse<InvoiceSummaryDto> list(Authentication auth,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) InvoiceSource source,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tối đa 100 ký tự.") String keyword,
            @RequestParam(required = false) @Positive(message = "ID chi nhánh phải lớn hơn 0.") Long branchId,
            @Parameter(description = "Trang từ 0") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Trang phải từ 0.") int page,
            @Parameter(description = "Số hóa đơn mỗi trang, từ 1 đến 100") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100.")
            @Max(value = 100, message = "Kích thước trang phải từ 1 đến 100.") int size) {
        return invoices.search(CurrentUser.from(auth), status, source, fromDate, toDate, keyword, branchId, page, size);
    }

    @GetMapping("/branches")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Danh mục chi nhánh để Chủ chuỗi lọc hóa đơn (AC-F5.3.1-04)",
        description = "Chỉ đọc id, mã và tên; gồm chi nhánh ngừng hoạt động để tra cứu hóa đơn lịch sử.")
    @ApiResponse(responseCode = "200", description = "Danh mục chi nhánh sắp theo mã")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải Chủ chuỗi")
    public List<InvoiceBranchDto> branches() {
        return invoices.branchesForFilter();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'BRANCH_MANAGER', 'SUPER_ADMIN')")
    @Operation(summary = "Xem chi tiết một hóa đơn kèm các dòng hóa đơn và phiếu thu (UC-F5.3)",
        description = "Ngoài chi nhánh trả 404 như không tồn tại (BR-F5-06).")
    @ApiResponse(responseCode = "200", description = "Chi tiết hóa đơn")
    @ApiResponse(responseCode = "400", description = "ID hóa đơn không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân, quản lý chi nhánh hay chủ chuỗi")
    @ApiResponse(responseCode = "404", description = "Hóa đơn không tồn tại hoặc thuộc chi nhánh khác")
    public InvoiceDetailDto detail(Authentication auth,
            @PathVariable @Positive(message = "ID hóa đơn phải lớn hơn 0.") Long id) {
        return invoices.detail(CurrentUser.from(auth), id);
    }
}
