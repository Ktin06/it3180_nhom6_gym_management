package vn.edu.hust.nhom6.gym.feature.notification.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationDto;
import vn.edu.hust.nhom6.gym.feature.notification.dto.UnreadNotificationCountDto;
import vn.edu.hust.nhom6.gym.feature.notification.service.MemberNotificationService;

/**
 * API thông báo trong web cho hội viên (UC-F10.5, BR-F10.7, BR-F10.8). Mọi endpoint lấy tài khoản từ JWT qua
 * {@link CurrentUser#from(Authentication)}, không nhận {@code userId} từ request.
 */
@RestController
@RequestMapping("/api/v1/me/notifications")
@Tag(name = "F10 – Thông báo")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MEMBER')")
public class NotificationController {
    private final MemberNotificationService notifications;

    @GetMapping
    @Operation(summary = "Xem danh sách thông báo trong web của tôi (UC-F10.5)",
        description = "Chỉ trả kênh IN_APP của chính tài khoản đăng nhập, mới nhất trước, phân trang.")
    @ApiResponse(responseCode = "200", description = "Trang danh sách thông báo, có thể rỗng")
    @ApiResponse(responseCode = "400", description = "Tham số phân trang không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên")
    public PageResponse<NotificationDto> list(Authentication auth,
            @Parameter(description = "Trang từ 0") @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Trang phải từ 0.") int page,
            @Parameter(description = "Số bản ghi mỗi trang, từ 1 đến 100") @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Kích thước trang phải từ 1 đến 100.")
            @Max(value = 100, message = "Kích thước trang phải từ 1 đến 100.") int size) {
        return notifications.list(CurrentUser.from(auth), page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Đếm số thông báo trong web chưa đọc (UC-F10.5)",
        description = "Đếm các dòng IN_APP có read_at rỗng của chính tài khoản đăng nhập.")
    @ApiResponse(responseCode = "200", description = "Số thông báo chưa đọc")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên")
    public UnreadNotificationCountDto unreadCount(Authentication auth) {
        return notifications.unreadCount(CurrentUser.from(auth));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu thông báo đã đọc (UC-F10.5 A2)",
        description = "Ghi thời điểm read_at cho thông báo IN_APP của chính hội viên. Thông báo của người khác trả 403, "
            + "không tồn tại trả 404.")
    @ApiResponse(responseCode = "200", description = "Thông báo sau khi đánh dấu đã đọc")
    @ApiResponse(responseCode = "400", description = "Mã thông báo không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên hoặc thông báo của người khác")
    @ApiResponse(responseCode = "404", description = "Thông báo không tồn tại")
    public NotificationDto markRead(Authentication auth,
            @Parameter(description = "Mã thông báo") @PathVariable
            @Positive(message = "Mã thông báo phải là số nguyên dương.") Long id) {
        return notifications.markRead(CurrentUser.from(auth), id);
    }
}
