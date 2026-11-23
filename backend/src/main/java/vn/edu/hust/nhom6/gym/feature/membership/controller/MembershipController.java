package vn.edu.hust.nhom6.gym.feature.membership.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
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
    public List<HeldMembershipDto> list(Authentication auth,
            @RequestParam @Positive(message = "ID hội viên phải lớn hơn 0.") Long memberId) {
        return memberships.listForStaff(CurrentUser.from(auth), memberId);
    }
}
