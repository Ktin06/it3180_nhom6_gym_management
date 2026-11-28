package vn.edu.hust.nhom6.gym.feature.trainer.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionBookRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionCancelRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionDto;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtSessionService;

/** API đặt buổi PT (UC-F9.2) và hủy buổi PT (UC-F9.7). */
@RestController
@RequestMapping("/api/v1/pt-sessions")
@Tag(name = "F9 – Buổi PT")
@RequiredArgsConstructor
public class PtSessionController {
    private final PtSessionService sessions;

    @PostMapping
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên đặt buổi PT 60 phút trong giờ PT rảnh (UC-F9.2)",
        description = "Mã hội viên lấy từ token. Chỉ giữ chỗ, không trừ buổi của gói (BR-F9.5).")
    @ApiResponse(responseCode = "201", description = "Đã đặt buổi, trạng thái BOOKED")
    @ApiResponse(responseCode = "400", description = "Thiếu huấn luyện viên hoặc giờ bắt đầu, sai định dạng (E1)")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên (E2)")
    @ApiResponse(responseCode = "404", description = "Huấn luyện viên không tồn tại (E3)")
    @ApiResponse(responseCode = "409", description = "Một yêu cầu khác vừa đặt cùng khung giờ (E4)")
    @ApiResponse(responseCode = "422", description = "Không có gói PT, ngoài giờ rảnh, sát giờ, trùng lịch, "
        + "hoặc đã đặt đủ số buổi còn lại (E5)")
    public ResponseEntity<PtSessionDto> book(Authentication auth, @Valid @RequestBody PtSessionBookRequest request) {
        PtSessionDto result = sessions.book(CurrentUser.from(auth), request);
        return ResponseEntity.created(URI.create("/api/v1/pt-sessions/" + result.id())).body(result);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST','BRANCH_MANAGER')")
    @Operation(summary = "Hủy buổi PT (UC-F9.7)",
        description = "Hội viên, lễ tân: còn ít nhất PT_MIN_LEAD_HOURS giờ. Quản lý chi nhánh: trước giờ bắt đầu, "
            + "bắt buộc reason, ghi audit_logs và báo hội viên. Huấn luyện viên không tự hủy (BR-F9.17).")
    @ApiResponse(responseCode = "200", description = "Đã hủy, trạng thái CANCELLED; gói không đổi")
    @ApiResponse(responseCode = "400", description = "Quản lý không nhập lý do (E6)")
    @ApiResponse(responseCode = "403", description = "Buổi của hội viên khác, chi nhánh khác, hoặc vai trò khác (E1)")
    @ApiResponse(responseCode = "404", description = "Buổi không tồn tại (E2)")
    @ApiResponse(responseCode = "422", description = "Buổi không còn BOOKED (E3), hủy sát giờ (E4), đã tới giờ (E5)")
    public PtSessionDto cancel(Authentication auth,
            @PathVariable @Positive(message = "Mã buổi tập không hợp lệ.") Long id,
            @Valid @RequestBody(required = false) PtSessionCancelRequest request) {
        return sessions.cancel(CurrentUser.from(auth), id, request == null ? null : request.reason());
    }
}
