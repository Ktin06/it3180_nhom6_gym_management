package vn.edu.hust.nhom6.gym.feature.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.GlobalExceptionHandler;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.config.SecurityConfig;
import vn.edu.hust.nhom6.gym.feature.notification.controller.NotificationController;
import vn.edu.hust.nhom6.gym.feature.notification.dto.NotificationResponse;
import vn.edu.hust.nhom6.gym.feature.notification.dto.UnreadCountResponse;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;

@WebMvcTest(controllers = NotificationController.class)
@ContextConfiguration(classes = {
    NotificationController.class,
    SecurityConfig.class,
    GlobalExceptionHandler.class
})
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    @DisplayName("UC-F10.5: hội viên xem danh sách thông báo và số chưa đọc trả 200")
    void hoiVienXemDanhSachVaSoChuaDoc() throws Exception {
        var item = new NotificationResponse(
            1L, "MEMBERSHIP_EXPIRING", "Gói sắp hết hạn", "Nội dung",
            "memberships", 10L, null, OffsetDateTime.parse("2026-11-26T02:00:00Z"));
        when(notificationService.listMyNotifications(any(), eq(0), eq(20)))
            .thenReturn(new PageResponse<>(List.of(item), 0, 20, 1, 1));
        when(notificationService.unreadCount(any())).thenReturn(new UnreadCountResponse(1));

        mockMvc.perform(get("/api/v1/me/notifications")
                .with(jwt().jwt(j -> j.subject("15").claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].type").value("MEMBERSHIP_EXPIRING"));

        mockMvc.perform(get("/api/v1/me/notifications/unread-count")
                .with(jwt().jwt(j -> j.subject("15").claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @DisplayName("UC-F10.5 E2: vai trò không phải MEMBER gọi API thông báo trả 403")
    void vaiTroKhongPhaiMemberTra403() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications")
                .with(jwt().jwt(j -> j.subject("2").claim("role", "RECEPTIONIST"))
                    .authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("UC-F10.5: chưa đăng nhập gọi API thông báo trả 401")
    void chuaDangNhapTra401() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UC-F10.5 E1: tham số phân trang sai trả 400")
    void phanTrangSaiTra400() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications?page=-1&size=200")
                .with(jwt().jwt(j -> j.subject("15").claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UC-F10.5 E2/E3: đánh dấu đã đọc của người khác trả 403, không tồn tại trả 404")
    void danhDauDaDocTra403Va404() throws Exception {
        when(notificationService.markAsRead(any(), eq(99L)))
            .thenThrow(new ForbiddenException(NotificationService.NOTIFICATION_FORBIDDEN_MESSAGE));
        when(notificationService.markAsRead(any(), eq(404L)))
            .thenThrow(new NotFoundException(NotificationService.NOTIFICATION_NOT_FOUND_MESSAGE));

        mockMvc.perform(patch("/api/v1/me/notifications/99/read")
                .with(jwt().jwt(j -> j.subject("15").claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/me/notifications/404/read")
                .with(jwt().jwt(j -> j.subject("15").claim("role", "MEMBER"))
                    .authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
            .andExpect(status().isNotFound());
    }
}
