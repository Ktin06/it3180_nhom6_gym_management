package vn.edu.hust.nhom6.gym.feature.trainer.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionCompletionDto;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtSessionCompletionService;
import vn.edu.hust.nhom6.gym.feature.trainer.service.PtSessionCompletionService.Outcome;

/** API xác nhận buổi PT đã diễn ra và ghi vắng (UC-F9.3, BR-F9.18, BR-F9.19). */
@RestController
@RequestMapping("/api/v1/pt-sessions")
@Tag(name = "F9 – Buổi PT")
@RequiredArgsConstructor
public class PtSessionCompletionController {
    private final PtSessionCompletionService completions;

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('TRAINER','BRANCH_MANAGER')")
    @Operation(summary = "Xác nhận buổi PT đã diễn ra (UC-F9.3)",
        description = "DONE, trừ 1 buổi, chốt hoa hồng. PT được phân công làm từ giờ bắt đầu tới 23:59 cùng ngày giờ "
            + "Việt Nam; sau đó chỉ quản lý chi nhánh của buổi (ghi audit_logs). Buổi cuối: gói EXPIRED, gói PT nối "
            + "tiếp ACTIVE từ hôm nay, buổi tương lai chuyển sang gói đó hoặc bị hủy.")
    @ApiResponse(responseCode = "200", description = "Đã xác nhận")
    @ApiResponse(responseCode = "403", description = "Không phải PT được phân công, hoặc quản lý chi nhánh khác (E2)")
    @ApiResponse(responseCode = "404", description = "Buổi không tồn tại (E3)")
    @ApiResponse(responseCode = "409", description = "Thao tác khác vừa xác nhận, ghi vắng hoặc hủy buổi (E4)")
    @ApiResponse(responseCode = "422", description = "Chưa tới giờ, đã xác nhận, đã hủy, quá hạn, gói không trừ được (E5)")
    public PtSessionCompletionDto confirm(Authentication auth,
            @PathVariable @Positive(message = "Mã buổi tập không hợp lệ.") Long id) {
        return completions.complete(CurrentUser.from(auth), id, Outcome.DONE);
    }

    @PatchMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('TRAINER','BRANCH_MANAGER')")
    @Operation(summary = "Ghi hội viên vắng mặt (BR-F9.18)",
        description = "NO_SHOW, vẫn trừ 1 buổi, không hoa hồng. Quyền, hạn và buổi cuối như xác nhận.")
    @ApiResponse(responseCode = "200", description = "Đã ghi vắng")
    @ApiResponse(responseCode = "403", description = "Không phải PT được phân công, hoặc quản lý chi nhánh khác")
    @ApiResponse(responseCode = "404", description = "Buổi không tồn tại")
    @ApiResponse(responseCode = "409", description = "Thao tác khác vừa xác nhận, ghi vắng hoặc hủy buổi")
    @ApiResponse(responseCode = "422", description = "Chưa tới giờ, đã xác nhận, đã hủy, quá hạn, gói không trừ được")
    public PtSessionCompletionDto noShow(Authentication auth,
            @PathVariable @Positive(message = "Mã buổi tập không hợp lệ.") Long id) {
        return completions.complete(CurrentUser.from(auth), id, Outcome.NO_SHOW);
    }
}
