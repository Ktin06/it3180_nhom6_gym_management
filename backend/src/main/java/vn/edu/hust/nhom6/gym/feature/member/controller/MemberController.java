package vn.edu.hust.nhom6.gym.feature.member.controller;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.feature.member.dto.*;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
import vn.edu.hust.nhom6.gym.feature.member.service.*;

@RestController
@RequestMapping("/api/v1/members")
@Tag(name = "F2 – Hồ sơ hội viên")
@RequiredArgsConstructor
public class MemberController {
    private final MemberManagementService members;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_MANAGER','RECEPTIONIST')")
    @Operation(summary = "Tạo hồ sơ hội viên tại quầy, chưa có ảnh đại diện")
    public ResponseEntity<MemberDetailResponse> create(Authentication auth, @Valid @RequestBody CreateMemberRequest request) {
        var result = members.create(MemberActor.from(auth), request);
        return ResponseEntity.created(URI.create("/api/v1/members/" + result.id())).body(result);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_MANAGER','RECEPTIONIST')")
    @Operation(summary = "Tìm hội viên theo mã, điện thoại hoặc tiền tố họ tên trong phạm vi chi nhánh",
        description = "Mã HV + 6 chữ số không phân biệt hoa/thường; điện thoại đầy đủ hoặc tiền tố đầu 0 (ví dụ 0912). "
            + "Dãy số không khớp (ngắn như 5678, 84912 hoặc dài nhưng không phải số điện thoại hợp lệ) thì trả trang rỗng. "
            + "Số đầy đủ hỗ trợ chuẩn hóa +84; page từ 0; size từ 1 đến 100, mặc định 20; "
            + "sắp xếp id giảm dần. Loại hồ sơ đã xóa mềm.")
    public PageResponse<MemberSummaryResponse> search(Authentication auth,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) MemberStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải từ 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang từ 1 đến 100")
            @Max(value = 100, message = "Kích thước trang từ 1 đến 100") int size) {
        return members.search(MemberActor.from(auth), keyword, status, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_MANAGER','RECEPTIONIST','MEMBER')")
    @Operation(summary = "Xem hồ sơ hội viên; ngoài phạm vi trả 404, TRAINER tạm chưa được phép")
    public MemberDetailResponse detail(Authentication auth,
            @PathVariable @Positive(message = "ID hội viên phải lớn hơn 0") Long id) {
        return members.detail(MemberActor.from(auth), id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_MANAGER','RECEPTIONIST')")
    @Operation(summary = "Cập nhật đầy đủ thông tin được phép và trạng thái hội viên")
    public MemberDetailResponse update(Authentication auth,
            @PathVariable @Positive(message = "ID hội viên phải lớn hơn 0") Long id,
            @Valid @RequestBody UpdateMemberRequest request) {
        return members.update(MemberActor.from(auth), id, request);
    }
}
