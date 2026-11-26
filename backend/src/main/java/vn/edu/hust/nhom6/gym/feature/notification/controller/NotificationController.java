package vn.edu.hust.nhom6.gym.feature.notification.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationResponse;
import vn.edu.hust.nhom6.gym.feature.notification.dto.UnreadCountResponse;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;

/** API thông báo trong web cho hội viên (F10, UC-F10.5). */
@Validated
@RestController
@RequestMapping("/api/v1/me/notifications")
@PreAuthorize("hasRole('MEMBER')")
@Tag(name = "F10 – Thông báo")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notifications;

    @GetMapping
    @Operation(summary = "Xem danh sách thông báo trong web của hội viên hiện tại (mới nhất trước)")
    public PageResponse<NotificationResponse> list(
            Authentication auth,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Trang phải từ 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Kích thước trang từ 1 đến 100")
            @Max(value = 100, message = "Kích thước trang từ 1 đến 100") int size) {
        return notifications.listMyNotifications(CurrentUser.from(auth), page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Đếm số thông báo trong web chưa đọc của hội viên hiện tại")
    public UnreadCountResponse unreadCount(Authentication auth) {
        return notifications.unreadCount(CurrentUser.from(auth));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo trong web là đã đọc (chỉ chủ thông báo được phép)")
    public NotificationResponse markAsRead(
            Authentication auth,
            @PathVariable @Positive(message = "ID thông báo phải lớn hơn 0") Long id) {
        return notifications.markAsRead(CurrentUser.from(auth), id);
    }
}
