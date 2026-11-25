package vn.edu.hust.nhom6.gym.feature.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.time.MutableClock;
import vn.edu.hust.nhom6.gym.common.time.MutableClockConfiguration;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.trainer.service.TrainerAvailabilityService;

/**
 * Kiểm thử tích hợp API lịch rảnh và lịch dạy của huấn luyện viên (F9, S2-19.1, #193):
 * {@code /api/v1/pt/availabilities}, {@code /api/v1/pt/schedule}, {@code /api/v1/trainers/{trainerId}/availabilities}.
 *
 * <p>Mốc thời gian mặc định: thứ Sáu 20/11/2026 09:00 giờ Việt Nam; thứ Hai tới là 23/11, thứ Ba là 24/11.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class TrainerAvailabilityApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService tokens;
    @Autowired MutableClock clock;
    @Autowired VnTime vnTime;
    @Autowired JsonMapper json;
    @Autowired TrainerAvailabilityService availabilityService;

    TrainerFixtures f;
    Long pt1UserId;
    Long pt1StaffId;
    Long pt2UserId;
    Long pt2StaffId;
    Long memberUserId;
    Long receptionistUserId;
    Long receptionistStaffId;

    @BeforeEach
    void setUp() {
        clock.setVn(LocalDateTime.of(2026, 11, 20, 9, 0));
        f = new TrainerFixtures(jdbc, tokens);
        f.reset();
        pt1UserId = f.user("pt1@example.com", UserRole.TRAINER, f.cn01);
        pt1StaffId = f.staffId(pt1UserId);
        pt2UserId = f.user("pt2@example.com", UserRole.TRAINER, f.cn01);
        pt2StaffId = f.staffId(pt2UserId);
        memberUserId = f.user("member@example.com", UserRole.MEMBER, null);
        receptionistUserId = f.user("letan@example.com", UserRole.RECEPTIONIST, f.cn01);
        receptionistStaffId = f.staffId(receptionistUserId);
    }

    private Map<String, Object> body(Integer dayOfWeek, String startTime, String endTime) {
        var map = new LinkedHashMap<String, Object>();
        map.put("dayOfWeek", dayOfWeek);
        map.put("startTime", startTime);
        map.put("endTime", endTime);
        return map;
    }

    private ResultActions createSlot(Long userId, UserRole role, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/v1/pt/availabilities")
            .with(f.bearer(userId, role))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
    }

    private ResultActions updateSlot(Long userId, UserRole role, Long id, Map<String, Object> body) throws Exception {
        return mvc.perform(put("/api/v1/pt/availabilities/{id}", id)
            .with(f.bearer(userId, role))
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body)));
    }

    private ResultActions deleteSlot(Long userId, UserRole role, Long id) throws Exception {
        return mvc.perform(delete("/api/v1/pt/availabilities/{id}", id)
            .with(f.bearer(userId, role)));
    }

    private Long createOk(Long userId, int dayOfWeek, String start, String end) throws Exception {
        String content = createSlot(userId, UserRole.TRAINER, body(dayOfWeek, start, end))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(content).get("id").asLong();
    }

    @Test
    @DisplayName("US-F9.1.1 KB1: PT thêm khung rảnh hợp lệ thì 201, lấy trainerId từ token và giữ nguyên giờ VN")
    void createAvailabilityTakesTrainerFromTokenAndPreservesVnTime() throws Exception {
        var payload = body(2, "18:30", "21:00");
        payload.put("trainerId", pt2StaffId); // cố tình gửi trainerId PT khác -> phải bị bỏ qua, lấy từ token
        payload.put("branchId", f.cn01);

        createSlot(pt1UserId, UserRole.TRAINER, payload)
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", startsWith("/api/v1/pt/availabilities/")))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.trainerId").value(pt1StaffId))
            .andExpect(jsonPath("$.branchId").value(f.cn01))
            .andExpect(jsonPath("$.dayOfWeek").value(2))
            .andExpect(jsonPath("$.startTime").value("18:30:00"))
            .andExpect(jsonPath("$.endTime").value("21:00:00"));

        String rawStart = jdbc.queryForObject(
            "SELECT to_char(start_time, 'HH24:MI:SS') FROM trainer_availabilities WHERE trainer_id = ?",
            String.class, pt1StaffId);
        String rawEnd = jdbc.queryForObject(
            "SELECT to_char(end_time, 'HH24:MI:SS') FROM trainer_availabilities WHERE trainer_id = ?",
            String.class, pt1StaffId);
        assertThat(rawStart).isEqualTo("18:30:00");
        assertThat(rawEnd).isEqualTo("21:00:00");

        // Bỏ trống branchId -> mặc định lấy chi nhánh làm việc của PT; gửi chi nhánh khác -> 403
        createSlot(pt1UserId, UserRole.TRAINER, body(3, "08:00", "11:00"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.branchId").value(f.cn01));

        var otherBranch = body(4, "08:00", "11:00");
        otherBranch.put("branchId", f.cn02);
        createSlot(pt1UserId, UserRole.TRAINER, otherBranch)
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-F9.1.1 VD-13: Đọc lại khung giờ 18:30 do SQL ghi sẵn không bị lệch múi giờ")
    void readSqlSeededTimeColumnWithoutTimezoneShift() throws Exception {
        f.insertAvailabilityRaw(pt1StaffId, f.cn01, 2, "18:30:00", "20:30:00");

        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", pt1StaffId)
                .with(f.bearer(memberUserId, UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].startTime").value("18:30:00"))
            .andExpect(jsonPath("$[0].endTime").value("20:30:00"));

        mvc.perform(get("/api/v1/pt/schedule").with(f.bearer(pt1UserId, UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availabilities[0].startTime").value("18:30:00"))
            .andExpect(jsonPath("$.availabilities[0].endTime").value("20:30:00"));
    }

    @Test
    @DisplayName("US-F9.1.1 E1: Giờ kết thúc không sau giờ bắt đầu, thiếu trường hoặc thứ ngoài 1-7 thì 400")
    void invalidInputReturns400() throws Exception {
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "10:00", "10:00"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Giờ kết thúc phải sau giờ bắt đầu."));
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "12:00", "08:00"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Giờ kết thúc phải sau giờ bắt đầu."));
        createSlot(pt1UserId, UserRole.TRAINER, body(0, "08:00", "12:00"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.dayOfWeek").isString());
        createSlot(pt1UserId, UserRole.TRAINER, body(8, "08:00", "12:00"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.dayOfWeek").isString());
        createSlot(pt1UserId, UserRole.TRAINER, body(2, null, "12:00"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.startTime").isString());
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "08:00", null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.endTime").isString());

        assertThat(f.count("trainer_availabilities")).isZero();
    }

    @Test
    @DisplayName("US-F9.1.1 E4: Khung chồng khung cùng PT cùng thứ thì 422; khung nối tiếp và PT khác trùng giờ thì 201")
    void overlapSameTrainerIs422ButAdjacentOrOtherTrainerIs201() throws Exception {
        createOk(pt1UserId, 2, "08:00", "12:00");

        // Cùng PT, cùng thứ, chồng khung -> 422 E4
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "11:00", "14:00"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Khung giờ bị trùng với khung rảnh đã khai báo."));
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "09:00", "10:00"))
            .andExpect(status().isUnprocessableContent());
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "07:00", "13:00"))
            .andExpect(status().isUnprocessableContent());

        // Cùng PT, cùng thứ, nối tiếp chạm biên -> 201
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "12:00", "16:00"))
            .andExpect(status().isCreated());

        // Cùng PT, khác thứ, cùng giờ -> 201
        createSlot(pt1UserId, UserRole.TRAINER, body(3, "08:00", "12:00"))
            .andExpect(status().isCreated());

        // Hai PT khác nhau trùng giờ -> 201
        createSlot(pt2UserId, UserRole.TRAINER, body(2, "08:00", "12:00"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.trainerId").value(pt2StaffId));

        assertThat(f.count("trainer_availabilities")).isEqualTo(4);
    }

    @Test
    @DisplayName("US-F9.1.1 E4: Sửa khung chồng lên khung khác cùng PT thì 422, giữ nguyên khung cũ")
    void updateOverlapSameTrainerIs422() throws Exception {
        createOk(pt1UserId, 2, "08:00", "12:00");
        Long slot2 = createOk(pt1UserId, 2, "13:00", "17:00");

        updateSlot(pt1UserId, UserRole.TRAINER, slot2, body(2, "11:30", "17:00"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Khung giờ bị trùng với khung rảnh đã khai báo."));

        String startAfter = jdbc.queryForObject(
            "SELECT to_char(start_time, 'HH24:MI:SS') FROM trainer_availabilities WHERE id = ?",
            String.class, slot2);
        assertThat(startAfter).isEqualTo("13:00:00");

        // Sửa chạm biên (không chồng khung 1) và chồng lên giờ cũ của chính nó -> 200
        updateSlot(pt1UserId, UserRole.TRAINER, slot2, body(2, "12:00", "18:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value("12:00:00"))
            .andExpect(jsonPath("$.endTime").value("18:00:00"));
    }

    @Test
    @DisplayName("US-F9.1.1 E5: Xóa khung không còn buổi BOOKED tương lai thì 204 và ghi audit_logs; còn buổi BOOKED tương lai thì 422")
    void deleteBlockedByFutureBookedOtherwiseHardDeletesAndAudits() throws Exception {
        Long slotId = createOk(pt1UserId, 2, "18:00", "21:00");
        // Thứ Ba 24/11/2026 19:00-20:00 VN (tương lai so với 20/11 09:00)
        OffsetDateTime start = vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(19, 0));
        OffsetDateTime end = vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(20, 0));
        Long sessionId = f.insertPtSession(pt1StaffId, f.cn01, start, end, "BOOKED");

        deleteSlot(pt1UserId, UserRole.TRAINER, slotId)
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Khung giờ đã có hội viên đặt, không thể xóa hoặc thu hẹp."));
        assertThat(f.count("trainer_availabilities")).isEqualTo(1);
        assertThat(f.count("audit_logs")).isZero();

        // Hủy buổi PT -> không còn BOOKED -> xóa được, xóa hẳn dòng và ghi audit_logs
        jdbc.update("UPDATE pt_sessions SET status = 'CANCELLED', cancelled_at = now() WHERE id = ?", sessionId);
        deleteSlot(pt1UserId, UserRole.TRAINER, slotId)
            .andExpect(status().isNoContent());

        assertThat(f.count("trainer_availabilities")).isZero();
        assertThat(f.count("audit_logs")).isEqualTo(1);
        Map<String, Object> audit = jdbc.queryForMap("""
            SELECT user_id, branch_id, action, entity, entity_id, old_value::text AS old_value, new_value
            FROM audit_logs""");
        assertThat(((Number) audit.get("user_id")).longValue()).isEqualTo(pt1UserId);
        assertThat(((Number) audit.get("branch_id")).longValue()).isEqualTo(f.cn01);
        assertThat(audit.get("action")).isEqualTo("DELETE");
        assertThat(audit.get("entity")).isEqualTo("trainer_availabilities");
        assertThat(((Number) audit.get("entity_id")).longValue()).isEqualTo(slotId);
        var oldValue = json.readTree((String) audit.get("old_value"));
        assertThat(oldValue.get("trainerId").asLong()).isEqualTo(pt1StaffId);
        assertThat(oldValue.get("branchId").asLong()).isEqualTo(f.cn01);
        assertThat(oldValue.get("dayOfWeek").asInt()).isEqualTo(2);
        assertThat(oldValue.get("startTime").asString()).isEqualTo("18:00:00");
        assertThat(oldValue.get("endTime").asString()).isEqualTo("21:00:00");
        assertThat(audit.get("new_value")).isNull();
    }

    @Test
    @DisplayName("US-F9.1.1 E5: Buổi PT đã qua, DONE, CANCELLED hoặc NO_SHOW không chặn xóa khung")
    void pastOrNonBookedSessionsDoNotBlockDelete() throws Exception {
        Long slotId = createOk(pt1UserId, 2, "18:00", "21:00");

        // Buổi BOOKED đã qua (thứ Ba 17/11/2026 < 20/11/2026)
        f.insertPtSession(pt1StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 17), LocalTime.of(19, 0)),
            vnTime.at(LocalDate.of(2026, 11, 17), LocalTime.of(20, 0)), "BOOKED");
        // Buổi DONE, CANCELLED, NO_SHOW trong khung tương lai (24/11)
        f.insertPtSession(pt1StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(18, 0)),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(19, 0)), "DONE");
        f.insertPtSession(pt1StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(19, 0)),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(20, 0)), "CANCELLED");
        f.insertPtSession(pt1StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(20, 0)),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(21, 0)), "NO_SHOW");
        // Buổi BOOKED tương lai của PT khác trong cùng giờ
        f.insertPtSession(pt2StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(19, 0)),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(20, 0)), "BOOKED");

        deleteSlot(pt1UserId, UserRole.TRAINER, slotId)
            .andExpect(status().isNoContent());
        assertThat(f.count("trainer_availabilities")).isZero();
    }

    @Test
    @DisplayName("US-F9.1.1 E5: Sửa thu hẹp hoặc đổi thứ làm buổi BOOKED tương lai rơi ra ngoài thì 422; nới rộng thì 200")
    void updateNarrowBlockedByFutureBookedButWidenAllowed() throws Exception {
        Long slotId = createOk(pt1UserId, 2, "18:00", "21:00");
        f.insertPtSession(pt1StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(19, 0)),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(20, 0)), "BOOKED");

        // Thu hẹp về 18:00-19:00 (ví dụ SRS BR-F9.15) -> 422
        updateSlot(pt1UserId, UserRole.TRAINER, slotId, body(2, "18:00", "19:00"))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.detail").value("Khung giờ đã có hội viên đặt, không thể xóa hoặc thu hẹp."));
        // Thu hẹp cắt nửa buổi 18:00-19:30 -> 422
        updateSlot(pt1UserId, UserRole.TRAINER, slotId, body(2, "18:00", "19:30"))
            .andExpect(status().isUnprocessableContent());
        // Đổi sang thứ Tư -> 422
        updateSlot(pt1UserId, UserRole.TRAINER, slotId, body(3, "18:00", "21:00"))
            .andExpect(status().isUnprocessableContent());

        // Nới rộng thành 17:00-21:00 (ví dụ SRS BR-F9.15) -> 200
        updateSlot(pt1UserId, UserRole.TRAINER, slotId, body(2, "17:00", "21:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value("17:00:00"))
            .andExpect(jsonPath("$.endTime").value("21:00:00"));

        // Thu hẹp nhưng vẫn bao trọn buổi 19:00-20:00 -> 200
        updateSlot(pt1UserId, UserRole.TRAINER, slotId, body(2, "19:00", "20:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value("19:00:00"))
            .andExpect(jsonPath("$.endTime").value("20:00:00"));
    }

    @Test
    @DisplayName("US-F9.1.1 BR-F9.15: Buổi 06:30 sáng VN là ngày hôm trước theo UTC vẫn chặn đúng khung thứ của ngày VN")
    void vnTimezoneEarlyMorningBlocksCorrectVnWeekday() throws Exception {
        Long mondayNight = createOk(pt1UserId, 1, "22:00", "23:59");
        Long tuesdayMorning = createOk(pt1UserId, 2, "06:00", "08:00");

        // Thứ Ba 2026-11-24 06:30-07:30 VN == 2026-11-23T23:30:00Z (Thứ Hai theo UTC!)
        OffsetDateTime startUtc = OffsetDateTime.parse("2026-11-23T23:30:00Z");
        OffsetDateTime endUtc = OffsetDateTime.parse("2026-11-24T00:30:00Z");
        f.insertPtSession(pt1StaffId, f.cn01, startUtc, endUtc, "BOOKED");

        // Xóa và thu hẹp khung thứ Ba phải bị chặn 422
        deleteSlot(pt1UserId, UserRole.TRAINER, tuesdayMorning)
            .andExpect(status().isUnprocessableContent());
        updateSlot(pt1UserId, UserRole.TRAINER, tuesdayMorning, body(2, "07:00", "08:00"))
            .andExpect(status().isUnprocessableContent());

        // Xóa khung thứ Hai đêm không bị chặn -> 204
        deleteSlot(pt1UserId, UserRole.TRAINER, mondayNight)
            .andExpect(status().isNoContent());

        // Kiểm tra hàm dùng chung cho S2-19.2 (BR-F9.2) tại biên 06:30 VN
        assertThat(availabilityService.isWithinAvailability(pt1StaffId, startUtc, endUtc)).isTrue();
        assertThat(availabilityService.isWithinAvailability(pt1StaffId,
            OffsetDateTime.parse("2026-11-24T00:30:00Z"),
            OffsetDateTime.parse("2026-11-24T01:30:00Z"))).isFalse();
    }

    @Test
    @DisplayName("US-F9.1.1 E2, E3: PT sửa hoặc xóa khung của PT khác thì 403; khung không tồn tại thì 404")
    void otherTrainerIs403AndMissingSlotIs404() throws Exception {
        Long pt1Slot = createOk(pt1UserId, 2, "08:00", "12:00");

        updateSlot(pt2UserId, UserRole.TRAINER, pt1Slot, body(2, "09:00", "11:00"))
            .andExpect(status().isForbidden());
        deleteSlot(pt2UserId, UserRole.TRAINER, pt1Slot)
            .andExpect(status().isForbidden());

        updateSlot(pt1UserId, UserRole.TRAINER, 999_999L, body(2, "09:00", "11:00"))
            .andExpect(status().isNotFound());
        deleteSlot(pt1UserId, UserRole.TRAINER, 999_999L)
            .andExpect(status().isNotFound());

        assertThat(f.count("trainer_availabilities")).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"SUPER_ADMIN", "BRANCH_MANAGER", "RECEPTIONIST", "MEMBER"})
    @DisplayName("US-F9.1.1 E2: Vai trò không phải TRAINER gọi API /api/v1/pt/* thì 403")
    void nonTrainerCannotAccessPtEndpoints(UserRole role) throws Exception {
        Long slotId = createOk(pt1UserId, 2, "08:00", "12:00");
        Long caller = f.user("caller-" + role.name().toLowerCase() + "@example.com", role,
            role == UserRole.SUPER_ADMIN || role == UserRole.MEMBER ? null : f.cn01);

        createSlot(caller, role, body(3, "08:00", "10:00")).andExpect(status().isForbidden());
        updateSlot(caller, role, slotId, body(2, "09:00", "11:00")).andExpect(status().isForbidden());
        deleteSlot(caller, role, slotId).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/pt/schedule").with(f.bearer(caller, role)))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-F9.1.1 E2: Không có token thì 401; PT đã nghỉ việc (staff INACTIVE) thì 403")
    void anonymousIs401AndInactiveTrainerIs403() throws Exception {
        mvc.perform(post("/api/v1/pt/availabilities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body(2, "08:00", "12:00"))))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/pt/schedule")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", pt1StaffId))
            .andExpect(status().isUnauthorized());

        jdbc.update("UPDATE staff SET status = 'INACTIVE' WHERE id = ?", pt1StaffId);
        createSlot(pt1UserId, UserRole.TRAINER, body(2, "08:00", "12:00"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("US-F9.1.1 KB6: PT xem lịch dạy gồm khung rảnh, buổi PT và buổi lớp nhóm được phân công trong khoảng ngày VN")
    void trainerScheduleReturnsAvailabilitiesPtAndAssignedClassSessionsInVnRange() throws Exception {
        createOk(pt1UserId, 4, "14:00", "18:00");
        createOk(pt1UserId, 2, "08:00", "12:00");
        createOk(pt2UserId, 2, "08:00", "12:00"); // PT khác không xuất hiện

        // Buổi PT trong khoảng 2026-11-23..2026-11-29 (gồm 1 buổi sáng sớm 00:30 VN ngày 23/11 = 17:30 UTC ngày 22/11)
        Long ptEarly = f.insertPtSession(pt1StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 23), LocalTime.of(0, 30)),
            vnTime.at(LocalDate.of(2026, 11, 23), LocalTime.of(1, 30)), "BOOKED", "Tập sáng sớm");
        // Buổi PT ngoài khoảng (30/11 00:00 VN -> mốc mở dayEnd(29/11), không lấy)
        f.insertPtSession(pt1StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 30), LocalTime.of(0, 0)),
            vnTime.at(LocalDate.of(2026, 11, 30), LocalTime.of(1, 0)), "BOOKED");
        // Buổi PT của PT2 trong khoảng -> không lấy
        f.insertPtSession(pt2StaffId, f.cn01,
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(9, 0)),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(10, 0)), "BOOKED");

        // Buổi lớp nhóm: lịch gốc của PT2 nhưng buổi cụ thể phân công dạy thay cho PT1 (theo class_sessions.trainer_id)
        Long roomId = f.room(f.cn01, "Studio Yoga", 30);
        Long yogaId = f.classType("Yoga Cơ bản");
        Long scheduleOfPt2 = f.classSchedule(f.cn01, yogaId, pt2StaffId, roomId, 2,
            LocalTime.of(18, 0), LocalTime.of(19, 0), LocalDate.of(2026, 11, 1));
        Long subSessionForPt1 = f.classSession(scheduleOfPt2, f.cn01, pt1StaffId, roomId,
            LocalDate.of(2026, 11, 24),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(18, 0)),
            vnTime.at(LocalDate.of(2026, 11, 24), LocalTime.of(19, 0)), "SCHEDULED");
        // Buổi lớp nhóm của PT2 trong khoảng -> không lấy
        f.classSession(scheduleOfPt2, f.cn01, pt2StaffId, roomId,
            LocalDate.of(2026, 11, 25),
            vnTime.at(LocalDate.of(2026, 11, 25), LocalTime.of(18, 0)),
            vnTime.at(LocalDate.of(2026, 11, 25), LocalTime.of(19, 0)), "SCHEDULED");

        mvc.perform(get("/api/v1/pt/schedule")
                .param("from", "2026-11-23")
                .param("to", "2026-11-29")
                .with(f.bearer(pt1UserId, UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.trainerId").value(pt1StaffId))
            .andExpect(jsonPath("$.branchId").value(f.cn01))
            .andExpect(jsonPath("$.from").value("2026-11-23"))
            .andExpect(jsonPath("$.to").value("2026-11-29"))
            .andExpect(jsonPath("$.availabilities.length()").value(2))
            .andExpect(jsonPath("$.availabilities[0].dayOfWeek").value(2))
            .andExpect(jsonPath("$.availabilities[1].dayOfWeek").value(4))
            .andExpect(jsonPath("$.ptSessions.length()").value(1))
            .andExpect(jsonPath("$.ptSessions[0].id").value(ptEarly))
            .andExpect(jsonPath("$.ptSessions[0].memberName").value("Hội viên test"))
            .andExpect(jsonPath("$.ptSessions[0].status").value("BOOKED"))
            .andExpect(jsonPath("$.ptSessions[0].note").value("Tập sáng sớm"))
            .andExpect(jsonPath("$.classSessions.length()").value(1))
            .andExpect(jsonPath("$.classSessions[0].id").value(subSessionForPt1))
            .andExpect(jsonPath("$.classSessions[0].classTypeName").value("Yoga Cơ bản"))
            .andExpect(jsonPath("$.classSessions[0].roomName").value("Studio Yoga"))
            .andExpect(jsonPath("$.classSessions[0].status").value("SCHEDULED"));

        // Mặc định khi bỏ trống from, to -> 7 ngày từ hôm nay giờ VN (2026-11-20 .. 2026-11-26)
        mvc.perform(get("/api/v1/pt/schedule").with(f.bearer(pt1UserId, UserRole.TRAINER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.from").value("2026-11-20"))
            .andExpect(jsonPath("$.to").value("2026-11-26"))
            .andExpect(jsonPath("$.ptSessions.length()").value(1))
            .andExpect(jsonPath("$.classSessions.length()").value(1));

        // from > to -> 400
        mvc.perform(get("/api/v1/pt/schedule")
                .param("from", "2026-11-29")
                .param("to", "2026-11-23")
                .with(f.bearer(pt1UserId, UserRole.TRAINER)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Ngày bắt đầu không được sau ngày kết thúc."));
    }

    @Test
    @DisplayName("US-F9.2.1 KB: Hội viên xem khung rảnh của PT đang hoạt động thì 200; PT không tồn tại hoặc không phải TRAINER thì 404; vai trò khác thì 403")
    void memberReadsTrainerAvailabilitiesAndInvalidCases() throws Exception {
        createOk(pt1UserId, 5, "13:00", "17:00");
        createOk(pt1UserId, 2, "14:00", "18:00");
        createOk(pt1UserId, 2, "08:00", "12:00");

        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", pt1StaffId)
                .with(f.bearer(memberUserId, UserRole.MEMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].dayOfWeek").value(2))
            .andExpect(jsonPath("$[0].startTime").value("08:00:00"))
            .andExpect(jsonPath("$[1].dayOfWeek").value(2))
            .andExpect(jsonPath("$[1].startTime").value("14:00:00"))
            .andExpect(jsonPath("$[2].dayOfWeek").value(5));

        // PT không tồn tại -> 404
        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", 999_999L)
                .with(f.bearer(memberUserId, UserRole.MEMBER)))
            .andExpect(status().isNotFound());

        // Nhân viên không phải TRAINER (lễ tân) -> 404
        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", receptionistStaffId)
                .with(f.bearer(memberUserId, UserRole.MEMBER)))
            .andExpect(status().isNotFound());

        // PT đã ngừng hoạt động -> 404
        jdbc.update("UPDATE staff SET status = 'INACTIVE' WHERE id = ?", pt2StaffId);
        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", pt2StaffId)
                .with(f.bearer(memberUserId, UserRole.MEMBER)))
            .andExpect(status().isNotFound());

        // Vai trò không phải MEMBER -> 403
        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", pt1StaffId)
                .with(f.bearer(pt1UserId, UserRole.TRAINER)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/trainers/{trainerId}/availabilities", pt1StaffId)
                .with(f.bearer(receptionistUserId, UserRole.RECEPTIONIST)))
            .andExpect(status().isForbidden());
    }
}
