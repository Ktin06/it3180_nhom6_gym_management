package vn.edu.hust.nhom6.gym.feature.checkin.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinHistoryItemDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinRequest;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinResultDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinStatus;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckoutResultDto;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.MemberSearchDto;
import vn.edu.hust.nhom6.gym.feature.checkin.entity.CheckinMethod;
import vn.edu.hust.nhom6.gym.feature.checkin.service.CheckinService;

/**
 * Check-in tại quầy (F4). Chỉ lễ tân và quản lý chi nhánh; chủ chuỗi không thuộc chi nhánh nào nên bị 403 (UC-F4.1
 * bước 3). Check-out và lịch sử trong ngày thuộc S2-11.2.
 */
@RestController
@Validated
@RequestMapping("/api/v1/checkins")
@Tag(name = "F4 – Check-in tại quầy")
@RequiredArgsConstructor
public class CheckinController {
    private final CheckinService checkins;

    @GetMapping("/search-member")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','BRANCH_MANAGER')")
    @Operation(summary = "Tra cứu hội viên theo mã HV hoặc số điện thoại (UC-F4.1)",
        description = "Chỉ hội viên thuộc chi nhánh của nhân viên; chi nhánh khác trả 404. Hồ sơ ngừng hoạt động vẫn "
            + "trả 200 kèm status. Ảnh lấy qua GET /api/v1/members/{id}/avatar khi hasAvatar = true.")
    public MemberSearchDto search(Authentication auth, @RequestParam(required = false)
            @Size(max = 20, message = "Từ khóa tìm kiếm tối đa 20 ký tự.") String keyword) {
        return checkins.search(CurrentUser.from(auth), keyword);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('RECEPTIONIST','BRANCH_MANAGER')")
    @Operation(summary = "Check-in thủ công tại quầy (UC-F4.2, UC-F4.3)",
        description = "Server tự chọn gói (TIME trước SESSION; PT chỉ khi hôm nay có buổi PT), trừ tối đa một buổi "
            + "mỗi ngày, ghi arrived_at cho lớp nhóm bắt đầu trong [now − 15 phút, now + 60 phút].")
    public CheckinResultDto checkin(Authentication auth, @Valid @RequestBody CheckinRequest request) {
        return checkins.checkin(CurrentUser.from(auth), request.memberId());
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','BRANCH_MANAGER')")
    @Operation(summary = "Ghi nhận check-out cho lượt tập đang diễn ra (UC-F4.4)",
        description = "Chỉ lượt check-in đang mở thuộc chi nhánh của nhân viên; đã check-out, khác chi nhánh hoặc "
            + "không tồn tại đều trả 404. Trả thời lượng tập tính bằng phút (durationMinutes).")
    public CheckoutResultDto checkout(Authentication auth,
            @PathVariable @Positive(message = "ID lượt check-in phải lớn hơn 0.") Long id) {
        return checkins.checkout(CurrentUser.from(auth), id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','BRANCH_MANAGER')")
    @Operation(summary = "Lịch sử check-in trong ngày tại chi nhánh (UC-F4.5)",
        description = "Mặc định hôm nay theo ngày Việt Nam, lọc nửa mở trên checked_in_at để dùng chỉ mục "
            + "(branch_id, checked_in_at), mới nhất trước. Mỗi dòng trả hasAvatar thay cho avatarUrl (ADR 0011).")
    public PageResponse<CheckinHistoryItemDto> history(Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) CheckinStatus status,
            @RequestParam(required = false) CheckinMethod method,
            @RequestParam(required = false) @Size(max = 100, message = "Từ khóa tìm kiếm tối đa 100 ký tự.")
            String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải từ 0.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100.")
            @Max(value = 100, message = "Kích thước trang phải từ 1 đến 100.") int size) {
        return checkins.history(CurrentUser.from(auth), date, status, method, keyword, page, size);
    }
}
