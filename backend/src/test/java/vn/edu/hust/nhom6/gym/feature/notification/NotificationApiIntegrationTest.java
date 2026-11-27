package vn.edu.hust.nhom6.gym.feature.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.BookingFixtures.Account;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationRelatedEntity;
import vn.edu.hust.nhom6.gym.feature.notification.entity.NotificationType;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService;
import vn.edu.hust.nhom6.gym.feature.notification.service.NotificationService.CreatedNotifications;

/**
 * Kiểm thử tích hợp 3 API thông báo trong web cho hội viên (UC-F10.5, S2-22, #180):
 * {@code GET /api/v1/me/notifications}, {@code GET /api/v1/me/notifications/unread-count},
 * {@code PATCH /api/v1/me/notifications/{id}/read}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class NotificationApiIntegrationTest {
    static final LocalDateTime START_VN = LocalDateTime.of(2026, 11, 21, 9, 0);

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired NotificationService notificationService;

    NotificationFixtures f;

    @BeforeEach
    void setUp() {
        clock.setVn(START_VN);
        f = new NotificationFixtures(jdbc, tokens);
        f.reset();
    }

    @Test
    @DisplayName("TC-F10.5.1-02a: GET /api/v1/me/notifications chỉ trả IN_APP của chính mình, mới nhất trước, phân trang")
    void listReturnsOnlyOwnInAppNewestFirstPaginated() throws Exception {
        Account me = f.account(f.cn01);
        Account other = f.account(f.cn01);

        CreatedNotifications first = notificationService.create(me.userId(), NotificationType.MEMBERSHIP_EXPIRING,
            "Thông báo 1", "Nội dung 1", NotificationRelatedEntity.MEMBERSHIPS, 10L, f.cn01);
        clock.advance(Duration.ofMinutes(5));
        notificationService.create(other.userId(), NotificationType.CLASS_CANCELLED,
            "Thông báo người khác", "Không được lộ", NotificationRelatedEntity.CLASS_SESSIONS, 99L, f.cn01);
        clock.advance(Duration.ofMinutes(5));
        CreatedNotifications second = notificationService.create(me.userId(), NotificationType.CLASS_REMINDER,
            "Thông báo 2", "Nội dung 2", NotificationRelatedEntity.CLASS_BOOKINGS, 20L, f.cn01);
        clock.advance(Duration.ofMinutes(5));
        CreatedNotifications third = notificationService.create(me.userId(), NotificationType.CLASS_RESCHEDULED,
            "Thông báo 3", "Nội dung 3", NotificationRelatedEntity.CLASS_SESSIONS, 30L, f.cn01);

        mockMvc.perform(get("/api/v1/me/notifications")
                .param("page", "0")
                .param("size", "2")
                .with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value(third.inApp().getId()))
            .andExpect(jsonPath("$.content[0].type").value("CLASS_RESCHEDULED"))
            .andExpect(jsonPath("$.content[0].title").value("Thông báo 3"))
            .andExpect(jsonPath("$.content[0].content").value("Nội dung 3"))
            .andExpect(jsonPath("$.content[0].relatedEntity").value("class_sessions"))
            .andExpect(jsonPath("$.content[0].relatedId").value(30))
            .andExpect(jsonPath("$.content[0].read").value(false))
            .andExpect(jsonPath("$.content[0].readAt").value(nullValue()))
            .andExpect(jsonPath("$.content[0].createdAt").isString())
            .andExpect(jsonPath("$.content[1].id").value(second.inApp().getId()))
            .andExpect(jsonPath("$.content[1].relatedEntity").value("class_bookings"));

        mockMvc.perform(get("/api/v1/me/notifications")
                .param("page", "1")
                .param("size", "2")
                .with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(first.inApp().getId()))
            .andExpect(jsonPath("$.content[0].relatedEntity").value("memberships"));
    }

    @Test
    @DisplayName("TC-F10.5.1-01b: chưa có thông báo trả danh sách rỗng và unread-count bằng 0")
    void emptyNotificationsAndZeroUnreadCount() throws Exception {
        Account me = f.account(f.cn01);

        mockMvc.perform(get("/api/v1/me/notifications").with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));

        mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("TC-F10.5.1-04a: đánh dấu đã đọc ghi read_at, gọi lại giữ nguyên read_at, unread-count giảm đúng")
    void markReadSetsReadAtAndUpdatesUnreadCount() throws Exception {
        Account me = f.account(f.cn01);
        Account other = f.account(f.cn01);

        CreatedNotifications n1 = notificationService.create(me.userId(), NotificationType.CLASS_ROOM_CHANGED,
            "Đổi phòng", "Phòng mới", NotificationRelatedEntity.CLASS_SESSIONS, 11L, f.cn01);
        notificationService.create(me.userId(), NotificationType.CLASS_CANCELLED,
            "Hủy buổi", "Đã hủy", NotificationRelatedEntity.CLASS_SESSIONS, 12L, f.cn01);
        notificationService.create(other.userId(), NotificationType.CLASS_CANCELLED,
            "Của người khác", "Nội dung", NotificationRelatedEntity.CLASS_SESSIONS, 12L, f.cn01);

        mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(2));

        clock.advance(Duration.ofMinutes(10));
        String firstReadAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .format(NotificationFixtures.utc(START_VN.plusMinutes(10)));

        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", n1.inApp().getId())
                .with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(n1.inApp().getId()))
            .andExpect(jsonPath("$.read").value(true))
            .andExpect(jsonPath("$.readAt").value(firstReadAt));

        mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unreadCount").value(1));

        clock.advance(Duration.ofMinutes(5));
        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", n1.inApp().getId())
                .with(f.bookings.member(me)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.read").value(true))
            .andExpect(jsonPath("$.readAt").value(firstReadAt));
    }

    @Test
    @DisplayName("TC-F10.5.1-05c: đánh dấu đã đọc thông báo của người khác trả 403, không đổi read_at")
    void markReadOfAnotherMemberIsForbidden() throws Exception {
        Account owner = f.account(f.cn01);
        Account intruder = f.account(f.cn01);
        CreatedNotifications created = notificationService.create(owner.userId(), NotificationType.PT_SESSION_CANCELLED,
            "Hủy buổi PT", "Lý do", NotificationRelatedEntity.PT_SESSIONS, 77L, f.cn01);

        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", created.inApp().getId())
                .with(f.bookings.member(intruder)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        Object readAt = jdbc.queryForObject("SELECT read_at FROM notifications WHERE id = ?",
            Object.class, created.inApp().getId());
        assertThat(readAt).isNull();
    }

    @Test
    @DisplayName("TC-F10.5.1-06a: đánh dấu đã đọc thông báo không tồn tại hoặc dòng EMAIL trả 404")
    void markReadNotFoundOrEmailChannelReturns404() throws Exception {
        Account me = f.account(f.cn01);
        CreatedNotifications created = notificationService.create(me.userId(), NotificationType.CLASS_CANCELLED,
            "Hủy buổi", "Nội dung", NotificationRelatedEntity.CLASS_SESSIONS, 50L, f.cn01);

        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", 999_999L)
                .with(f.bookings.member(me)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(patch("/api/v1/me/notifications/{id}/read", created.email().getId())
                .with(f.bookings.member(me)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("TC-F10.5.1-06b: chưa đăng nhập 401, vai trò khác MEMBER 403, tham số phân trang hoặc id sai 400")
    void authAndValidationChecks() throws Exception {
        Account me = f.account(f.cn01);
        Long managerId = f.bookings.f.user("ql-noti@example.com", UserRole.BRANCH_MANAGER, f.cn01);
        Long trainerId = f.bookings.f.user("pt-noti@example.com", UserRole.TRAINER, f.cn01);
        Long ownerId = f.bookings.f.user("chu-noti@example.com", UserRole.SUPER_ADMIN, null);

        mockMvc.perform(get("/api/v1/me/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/me/notifications/1/read")).andExpect(status().isUnauthorized());

        for (RequestPostProcessor auth : new RequestPostProcessor[] {
                f.bookings.bearer(f.bookings.receptionist, UserRole.RECEPTIONIST),
                f.bookings.bearer(managerId, UserRole.BRANCH_MANAGER),
                f.bookings.bearer(trainerId, UserRole.TRAINER),
                f.bookings.bearer(ownerId, UserRole.SUPER_ADMIN)
        }) {
            mockMvc.perform(get("/api/v1/me/notifications").with(auth)).andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(auth)).andExpect(status().isForbidden());
            mockMvc.perform(patch("/api/v1/me/notifications/1/read").with(auth)).andExpect(status().isForbidden());
        }

        mockMvc.perform(get("/api/v1/me/notifications").param("page", "-1").with(f.bookings.member(me)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.page").isString());
        mockMvc.perform(get("/api/v1/me/notifications").param("size", "0").with(f.bookings.member(me)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());
        mockMvc.perform(get("/api/v1/me/notifications").param("size", "101").with(f.bookings.member(me)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.size").isString());
        mockMvc.perform(patch("/api/v1/me/notifications/0/read").with(f.bookings.member(me)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.id").isString());
    }
}
