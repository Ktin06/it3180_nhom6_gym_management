package vn.edu.hust.nhom6.gym.feature.membership.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.membership.dto.HeldMembershipDto;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;

@RestController
@RequestMapping("/api/v1/memberships")
@Tag(name = "F3 – Gói hội viên")
@RequiredArgsConstructor
public class MembershipController {
    private final MembershipService memberships;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_MANAGER','RECEPTIONIST')")
    @Operation(summary = "Gói PENDING, UPCOMING, ACTIVE, FROZEN của một hội viên, kèm ngày bắt đầu dự kiến nếu gia hạn",
        description = "renewalStartDate do server tính theo BR-F3-11; rỗng nếu đã có gói cùng loại đang chờ. "
            + "Hội viên ngoài chi nhánh của nhân viên trả 404.")
    @ApiResponse(responseCode = "200", description = "Danh sách gói; rỗng nếu chưa có gói đang giữ")
    @ApiResponse(responseCode = "400", description = "Thiếu memberId hoặc ID không phải số nguyên dương")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập hoặc token không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Vai trò không được phép hoặc thiếu chi nhánh nhân viên hợp lệ")
    @ApiResponse(responseCode = "404", description = "Hội viên không tồn tại hoặc ngoài phạm vi")
    public List<HeldMembershipDto> list(Authentication auth,
            @RequestParam @Positive(message = "ID hội viên phải lớn hơn 0.") Long memberId) {
        return memberships.listForStaff(CurrentUser.from(auth), memberId);
    }
}
