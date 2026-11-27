package vn.edu.hust.nhom6.gym.feature.trainer.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.ProgressDto;
import vn.edu.hust.nhom6.gym.feature.trainer.service.ProgressService;

/**
 * Tiến độ học viên (F9, UC-F9.6; S2-20.1): dữ liệu thô của chỉ số cơ thể và nhật ký tập theo ngày, không gợi ý AI
 * (BR-F9.13). Khoảng ngày Việt Nam tính cả hai đầu; bỏ trống thì 90 ngày đến hôm nay; tối đa 366 ngày.
 */
@RestController
@Tag(name = "F9 – Tiến độ học viên")
@RequiredArgsConstructor
public class ProgressController {
    private final ProgressService progress;

    @GetMapping("/api/v1/members/{memberId}/progress")
    @PreAuthorize("hasAnyRole('TRAINER','MEMBER')")
    @Operation(summary = "Xem tiến độ một hội viên: PT với học viên mình dạy, hội viên với chính mình")
    @ApiResponse(responseCode = "200", description = "Dữ liệu tiến độ, có thể rỗng")
    @ApiResponse(responseCode = "400", description = "from sau to, sai định dạng ngày, hoặc khoảng dài hơn 366 ngày")
    @ApiResponse(responseCode = "403", description = "PT không dạy hội viên này; vai trò khác PT, hội viên")
    @ApiResponse(responseCode = "404", description = "Không tồn tại, ngoài chi nhánh, hoặc hội viên xem người khác")
    public ProgressDto forMember(Authentication auth,
            @PathVariable @Positive(message = "ID hội viên phải lớn hơn 0") Long memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return progress.forMember(CurrentUser.from(auth), memberId, from, to);
    }

    @GetMapping("/api/v1/me/progress")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên xem tiến độ của chính mình (lấy từ token)")
    @ApiResponse(responseCode = "200", description = "Dữ liệu tiến độ, có thể rỗng")
    @ApiResponse(responseCode = "400", description = "from sau to, sai định dạng ngày, hoặc khoảng dài hơn 366 ngày")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên, hoặc tài khoản chưa gắn hồ sơ")
    public ProgressDto mine(Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return progress.mine(CurrentUser.from(auth), from, to);
    }
}
