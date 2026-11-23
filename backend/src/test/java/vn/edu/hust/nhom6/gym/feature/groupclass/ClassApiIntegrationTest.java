package vn.edu.hust.nhom6.gym.feature.groupclass;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged;
import vn.edu.hust.nhom6.gym.feature.groupclass.event.ClassSessionChanged.Kind;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.*;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PostgreSQL/Flyway thật; chỉ điều khiển đồng hồ, không mock nghiệp vụ hoặc khóa DB. */
@SpringBootTest(properties = "app.classes.generation.enabled=false")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ClassApiIntegrationTest.EventTestConfiguration.class})
@RecordApplicationEvents
class ClassApiIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JwtTokenService tokens;
    @Autowired ClassBookingService bookings;
    @Autowired ClassScheduleService schedules;
    @Autowired ClassSessionService sessions;
    @Autowired ClassAttendanceService attendance;
    @Autowired ApplicationEvents events;
    @Autowired PlatformTransactionManager transactions;
    @Autowired FailingNotificationListener notifications;
    @MockitoBean(name = "clock") Clock clock;
    static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-18T10:00:00+07:00");
    long branch, otherBranch, manager, receptionist, trainer, trainerUser, room, type, member, user;

    @BeforeEach
    void fixture() {
        // Giữ cấu hình toàn chuỗi đã qua Flyway khi reset dữ liệu trong PostgreSQL cô lập.
        jdbc.execute("""
            CREATE TEMP TABLE f8_default_configs ON COMMIT DROP AS
                SELECT config_key, config_value, description FROM system_configs WHERE branch_id IS NULL;
            TRUNCATE users, packages, class_types, rooms RESTART IDENTITY CASCADE;
            INSERT INTO system_configs(config_key, config_value, description)
                SELECT config_key, config_value, description FROM f8_default_configs;
            """);
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        jdbc.update("INSERT INTO branches(code,name,open_time,close_time) VALUES ('F8TEST','Khác','06:00','22:00') ON CONFLICT DO NOTHING");
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code='F8TEST'", Long.class);
        manager = user("manager", "BRANCH_MANAGER");
        receptionist = user("reception", "RECEPTIONIST");
        trainerUser = user("trainer", "TRAINER");
        trainer = staff(trainerUser, "PT");
        staff(manager, "QL"); staff(receptionist, "LT");
        room = jdbc.queryForObject("INSERT INTO rooms(branch_id,name,capacity) VALUES (?,'Studio',20) RETURNING id", Long.class, branch);
        type = jdbc.queryForObject("INSERT INTO class_types(name) VALUES ('Yoga') RETURNING id", Long.class);
        user = user("member", "MEMBER"); member = member(user);
        timePackage(member);
        at(NOW);
    }

    private void at(OffsetDateTime time) {
        when(clock.instant()).thenReturn(time.toInstant()); when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        doAnswer(i -> Clock.fixed(time.toInstant(), i.getArgument(0))).when(clock).withZone(any(ZoneId.class));
    }

    private long user(String name, String role) {
        return jdbc.queryForObject("INSERT INTO users(email,password_hash,role) VALUES (?,'test-only',?) RETURNING id",
            Long.class, name + "@f8.example", role);
    }

    private long staff(long userId, String code) {
        return jdbc.queryForObject("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,?,?) RETURNING id",
            Long.class, userId, branch, code, code);
    }

    private long member(Long userId) {
        return jdbc.queryForObject("""
            INSERT INTO members(user_id,home_branch_id,member_code,full_name,phone)
            VALUES (?,?,'HV'||lpad(nextval('member_code_seq')::text,6,'0'),'Hội viên',
                '0'||lpad(currval('member_code_seq')::text,9,'0')) RETURNING id
            """, Long.class, userId, branch);
    }

    private void timePackage(long memberId) {
        Long pkg = jdbc.queryForObject("INSERT INTO packages(code,name,type,duration_days,price) VALUES (?,'TIME','TIME',90,500000) RETURNING id",
            Long.class, "P" + memberId);
        jdbc.update("""
            INSERT INTO memberships(member_id,package_id,package_type,sold_branch_id,start_date,end_date,status)
            VALUES (?,?,'TIME',?,'2026-11-01','2026-12-31','ACTIVE')
            """, memberId, pkg, branch);
    }

    private long session(OffsetDateTime start, int capacity) {
        Long schedule = jdbc.queryForObject("""
            INSERT INTO class_schedules(branch_id,class_type_id,trainer_id,room_id,day_of_week,start_time,end_time,capacity,valid_from)
            VALUES (?,?,?,?,3,'13:00','14:00',?,'2026-11-01') RETURNING id
            """, Long.class, branch, type, trainer, room, capacity);
        return jdbc.queryForObject("""
            INSERT INTO class_sessions(class_schedule_id,branch_id,trainer_id,room_id,occurrence_date,start_at,end_at,capacity)
            VALUES (?,?,?,?,?,?,?,?) RETURNING id
            """, Long.class, schedule, branch, trainer, room, start.toLocalDate(), start, start.plusHours(1), capacity);
    }

    private CurrentUser actor() { return new CurrentUser(user, UserRole.MEMBER); }
    private CurrentUser manager() { return new CurrentUser(manager, UserRole.BRANCH_MANAGER); }
    private String bearer(long id, String role) { return "Bearer " + tokens.generateAccessToken(id, "test@f8.example", UserRole.valueOf(role)); }

    @Test
    @DisplayName("TC-F8.1.1-03a, TC-F8.3.1-04a: Lớp đầy trả 409; chỗ vừa hủy được đặt bằng yêu cầu mới")
    void fullClassRejectsBookingAndCancellationReleasesSeat() throws Exception {
        long session = session(NOW.plusHours(3), 1);
        var first = bookings.book(actor(), session, member);
        long secondUser = user("second", "MEMBER");
        long secondMember = member(secondUser);
        timePackage(secondMember);
        var second = new CurrentUser(secondUser, UserRole.MEMBER);

        mvc.perform(post("/api/v1/class-sessions/{id}/bookings", session)
            .header("Authorization", bearer(secondUser, "MEMBER")).contentType(APPLICATION_JSON)
            .content(json.writeValueAsString(new BookingRequest(secondMember))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.detail").value("Lớp đã đủ chỗ"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings WHERE class_session_id=?", Long.class, session))
            .isEqualTo(1);

        bookings.cancel(actor(), first.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings WHERE class_session_id=?", Long.class, session))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications", Long.class)).isZero();
        assertThat(bookings.book(second, session, secondMember).status()).isEqualTo("BOOKED");
    }

    @ParameterizedTest
    @DisplayName("TC-F8.1.1-03b: Các trạng thái chiếm chỗ đều tính vào sĩ số")
    @ValueSource(strings = {"BOOKED", "ATTENDED", "NO_SHOW"})
    void occupiedStatusesCountTowardsCapacity(String state) {
        long session = session(NOW.plusHours(3), 1);
        var first = bookings.book(actor(), session, member);
        jdbc.update("UPDATE class_bookings SET status=? WHERE id=?", state, first.id());
        long secondUser = user("second", "MEMBER");
        long secondMember = member(secondUser);
        timePackage(secondMember);

        assertThatThrownBy(() -> bookings.book(new CurrentUser(secondUser, UserRole.MEMBER), session, secondMember))
            .isInstanceOf(ConflictException.class).hasMessage("Lớp đã đủ chỗ");
    }

    @Test
    @DisplayName("TC-F8.1.1-06a: Hội viên đặt lại sau hủy và giữ lịch sử")
    void memberCanBookAgainAfterCancellation() {
        long session = session(NOW.plusHours(3), 1);
        var first = bookings.book(actor(), session, member);
        bookings.cancel(actor(), first.id());

        var second = bookings.book(actor(), session, member);
        assertThat(second.status()).isEqualTo("BOOKED");
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(bookings.detail(actor(), first.id()).status()).isEqualTo("CANCELLED");
    }

    @ParameterizedTest
    @DisplayName("TC-F8.1.1-02a: Từ chối gói không hợp lệ, hội viên ngừng hoạt động hoặc bị khóa")
    @ValueSource(strings = {"SESSION", "PT", "EXPIRED", "PENDING", "FROZEN", "WRONG_BRANCH", "ENDED", "LOCKED", "INACTIVE"})
    void rejectsIneligibleMemberships(String condition) {
        switch (condition) {
            case "SESSION", "PT" -> jdbc.update("UPDATE memberships SET package_type=?,total_sessions=10,remaining_sessions=10", condition);
            case "WRONG_BRANCH" -> jdbc.update("UPDATE memberships SET branch_id=?", otherBranch);
            case "ENDED" -> jdbc.update("UPDATE memberships SET end_date='2026-11-17'");
            case "LOCKED" -> jdbc.update("UPDATE members SET booking_locked_until=?", NOW.plusDays(1));
            case "INACTIVE" -> jdbc.update("UPDATE members SET status='INACTIVE'");
            default -> jdbc.update("UPDATE memberships SET status=?", condition);
        }
        long s = session(NOW.plusHours(3), 1);
        String reason = switch (condition) {
            case "LOCKED" -> "đang bị khóa";
            case "INACTIVE" -> "không ở trạng thái hoạt động";
            default -> "TIME";
        };
        assertThatThrownBy(() -> bookings.book(actor(), s, member)).hasMessageContaining(reason);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings", Long.class)).isZero();
    }

    @Test
    @DisplayName("TC-F8.1.1-07a: Cửa sổ theo ngày lịch Việt Nam và cấu hình chi nhánh")
    void dayWindowUsesVietnamDatesAndBranchOverrides() {
        long lastDay = session(NOW.plusDays(7).withHour(23), 1);
        assertThat(bookings.book(actor(), lastDay, member).status()).isEqualTo("BOOKED");
        long outside = session(NOW.plusDays(8), 1);
        assertThatThrownBy(() -> bookings.book(actor(), outside, member)).hasMessageContaining("cửa sổ");
        jdbc.update("INSERT INTO system_configs(branch_id,config_key,config_value) VALUES (?,'BOOKING_WINDOW_DAYS','8')", branch);
        assertThat(bookings.book(actor(), outside, member).status()).isEqualTo("BOOKED");
    }

    @ParameterizedTest
    @DisplayName("TC-F8.1.1-08a: Đúng mốc đóng được xét, dưới mốc từ chối đặt")
    @CsvSource({"30,true", "29,false"})
    void cutoffBoundary(long minutes, boolean permitted) {
        long s = session(NOW.plusMinutes(minutes), 1);
        if (permitted) { assertThat(bookings.book(actor(), s, member).status()).isEqualTo("BOOKED"); }
        else { assertThatThrownBy(() -> bookings.book(actor(), s, member)).hasMessageContaining("đóng đặt"); }
    }

    @Test
    @DisplayName("TC-F8.1.1-10a: Ưu tiên mốc đóng đặt của chi nhánh")
    void branchCutoffOverridesGlobalValue() {
        long session = session(NOW.plusMinutes(20), 1);
        assertThatThrownBy(() -> bookings.book(actor(), session, member)).hasMessageContaining("đóng đặt");

        jdbc.update("INSERT INTO system_configs(branch_id,config_key,config_value) VALUES (?,'BOOKING_CUTOFF_MIN','20')", branch);
        assertThat(bookings.book(actor(), session, member).status()).isEqualTo("BOOKED");
    }

    @ParameterizedTest
    @DisplayName("TC-F8.3.1-01a: Hủy sớm kể cả tại đúng mốc; hủy muộn dưới mốc")
    @CsvSource({"121,false", "120,false", "119,true"})
    void lateCancellationBoundary(long minutes, boolean late) {
        long s = session(NOW.plusMinutes(minutes), 1);
        var b = bookings.book(actor(), s, member);
        var cancelled = bookings.cancel(actor(), b.id());
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.isLateCancel()).isEqualTo(late);
        assertThatThrownBy(() -> bookings.cancel(actor(), b.id())).hasMessageContaining("không thể hủy");
    }

    @Test
    @DisplayName("TC-F8.3.1-03a: Hủy sát giờ sau khi buổi đổi lịch được miễn phạt")
    void cancellationAfterRescheduleIsNotLate() {
        long session = session(NOW.plusHours(1), 1);
        var booking = bookings.book(actor(), session, member);
        jdbc.update("""
            UPDATE class_sessions SET original_start_at=start_at,
                start_at=?, end_at=?, status='RESCHEDULED' WHERE id=?
            """, NOW.plusMinutes(2), NOW.plusMinutes(62), session);
        at(NOW.plusMinutes(1));

        var cancelled = bookings.cancel(actor(), booking.id());
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(cancelled.isLateCancel()).isFalse();
        assertThat(cancelled.bookingLockedUntil()).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action='CLASS_BOOKING_LOCK'", Long.class))
            .isZero();
    }

    @Test
    @DisplayName("TC-F8.3.1-02a, TC-F8.3.1-06a: Vi phạm thứ ba khóa 7 ngày và không tính lại lỗi cũ")
    void violationsLockOnceAndExpiredLockDoesNotReuseOldViolations() {
        for (int i = 0; i < 3; i++) {
            long s = session(NOW.plusMinutes(60 + i * 61), 1);
            var b = bookings.book(actor(), s, member);
            // Các buổi không giao nhau; hủy muộn tại thời điểm phù hợp của từng buổi.
            at(NOW.plusMinutes(i * 61));
            var cancelled = bookings.cancel(actor(), b.id());
            if (i == 2) {
                assertThat(cancelled.bookingLockedUntil().toInstant())
                    .isEqualTo(NOW.plusMinutes(122).plusDays(7).toInstant());
            } else {
                assertThat(cancelled.bookingLockedUntil()).isNull();
            }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action='CLASS_BOOKING_LOCK'", Long.class)).isEqualTo(1);
        long duringLock = session(NOW.plusDays(1), 1);
        assertThatThrownBy(() -> bookings.book(actor(), duringLock, member)).hasMessageContaining("đang bị khóa");
        at(NOW.plusDays(8));
        long s = session(NOW.plusDays(8).plusHours(1), 1);
        var b = bookings.book(actor(), s, member);
        assertThat(bookings.cancel(actor(), b.id()).isLateCancel()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action='CLASS_BOOKING_LOCK'", Long.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F8.3.1-05a: Sang tháng Việt Nam không cộng vi phạm tháng cũ")
    void oldCalendarMonthViolationsDoNotCountAtVietnamMidnight() {
        at(OffsetDateTime.parse("2026-12-01T00:00:00+07:00"));
        long old = session(NOW.minusDays(1), 1);
        jdbc.update("""
            INSERT INTO class_bookings(class_session_id,member_id,created_by,status,cancelled_at,cancel_reason,is_late_cancel)
            SELECT ?,?,?,'CANCELLED','2026-11-30T23:59:00+07:00','BY_MEMBER',true FROM generate_series(1,2)
            """, old, member, user);
        var start = OffsetDateTime.parse("2026-12-01T01:00:00+07:00");
        var b = bookings.book(actor(), session(start, 1), member);
        assertThat(bookings.cancel(actor(), b.id()).bookingLockedUntil()).isNull();
    }

    @Test
    @DisplayName("TC-F8.1.1-04a: Hai hội viên tranh chỗ cuối, một yêu cầu bị từ chối vì hết chỗ")
    void concurrentRequestsDoNotExceedCapacity() throws Exception {
        long session = session(NOW.plusHours(3), 1);
        long secondUser = user("racer", "MEMBER");
        long secondMember = member(secondUser);
        timePackage(secondMember);
        var result = race(() -> attemptBook(actor(), session, member),
            () -> attemptBook(new CurrentUser(secondUser, UserRole.MEMBER), session, secondMember));
        assertThat(result).containsExactlyInAnyOrder("BOOKED", "Lớp đã đủ chỗ");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings", Long.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("TC-F8.1.1-05a: Hai yêu cầu đồng thời của cùng hội viên vào hai buổi trùng giờ")
    void concurrentOverlappingRequestsRespectMemberLock() throws Exception {
        long left = session(NOW.plusDays(1), 1);
        long right = session(NOW.plusDays(1).plusMinutes(30), 1);
        assertThat(race(() -> attemptBook(actor(), left, member), () -> attemptBook(actor(), right, member)))
            .containsExactlyInAnyOrder("BOOKED", "Hội viên đã có lịch trùng giờ. Hãy hủy buổi kia trước.");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings", Long.class)).isEqualTo(1);
    }

    private String attemptBook(CurrentUser actor, long session, long member) {
        try {
            return bookings.book(actor, session, member).status();
        } catch (ConflictException ex) {
            return ex.getMessage();
        }
    }

    private List<String> race(Callable<String> left, Callable<String> right) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return left.call(); });
            var b = executor.submit(() -> { barrier.await(10, TimeUnit.SECONDS); return right.call(); });
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    private ScheduleRequest request(int day, LocalDate from, LocalDate to) {
        return new ScheduleRequest(type, trainer, room, day, LocalTime.of(13, 0), LocalTime.of(14, 0), 10, from, to);
    }

    @Test
    @DisplayName("TC-F8.8.1-01a, TC-F8.7.1-05a: Sinh đúng lịch và giữ buổi khi sửa hoặc ngừng lịch")
    void scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions() {
        var r = request(2, NOW.toLocalDate(), NOW.toLocalDate().plusDays(14));
        var s = schedules.save(manager(), null, r);
        assertThat(schedules.generate(s.id(), NOW.toLocalDate(), NOW.toLocalDate().plusDays(14))).isEqualTo(2);
        assertThat(schedules.generate(s.id(), NOW.toLocalDate(), NOW.toLocalDate().plusDays(14))).isZero();
        var bookedDay = NOW.toLocalDate().plusDays(6);
        long first = jdbc.queryForObject("SELECT id FROM class_sessions WHERE class_schedule_id=? AND occurrence_date=?",
            Long.class, s.id(), bookedDay);
        var start = jdbc.queryForObject("SELECT start_at FROM class_sessions WHERE id=?", OffsetDateTime.class, first);
        assertThat(start.toInstant()).isEqualTo(bookedDay.atTime(6, 0).toInstant(ZoneOffset.UTC));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_sessions WHERE trainer_id=? AND room_id=? AND branch_id=?",
            Long.class, trainer, room, branch)).isEqualTo(2);
        var booking = bookings.book(actor(), first, member);
        var changed = new ScheduleRequest(type, trainer, room, 2, LocalTime.of(15,0), LocalTime.of(16,0), 5, r.validFrom(), r.validTo());
        schedules.save(manager(), s.id(), changed);
        assertThat(jdbc.queryForObject("SELECT min(capacity) FROM class_sessions", Integer.class)).isEqualTo(10);
        schedules.deactivate(manager(), s.id());
        assertThat(schedules.generate(s.id(), NOW.toLocalDate(), NOW.toLocalDate().plusDays(14))).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_sessions", Long.class)).isEqualTo(2);
        assertThat(bookings.detail(actor(), booking.id()).status()).isEqualTo("BOOKED");
    }

    @Test
    @DisplayName("TC-F8.8.1-03a: Không sinh trước ngày hiệu lực, sau hạn hoặc khi lịch ngừng")
    void generationSkipsInactiveAndOutOfValidityDates() {
        LocalDate validDay = NOW.toLocalDate().plusWeeks(1);
        var schedule = schedules.save(manager(), null, request(3, validDay, validDay));
        assertThat(schedules.generate(schedule.id(), validDay.minusWeeks(1), validDay.minusWeeks(1))).isZero();
        assertThat(schedules.generate(schedule.id(), validDay.plusWeeks(1), validDay.plusWeeks(1))).isZero();

        schedules.deactivate(manager(), schedule.id());
        assertThat(schedules.generate(schedule.id(), validDay, validDay)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_sessions WHERE class_schedule_id=?",
            Long.class, schedule.id())).isZero();
    }

    @Test
    @DisplayName("TC-F8.8.1-05a: Tác vụ từ chối phòng ngừng hoạt động hoặc đã có buổi trùng giờ")
    void generationRejectsInactiveRoomOrConflictingSession() {
        LocalDate day = NOW.toLocalDate();
        var schedule = schedules.save(manager(), null, request(3, day, day));
        jdbc.update("UPDATE rooms SET status='INACTIVE' WHERE id=?", room);
        assertThatThrownBy(() -> schedules.generate(schedule.id(), day, day))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("không hợp lệ");

        jdbc.update("UPDATE rooms SET status='ACTIVE' WHERE id=?", room);
        session(NOW.withHour(13), 1);
        assertThatThrownBy(() -> schedules.generate(schedule.id(), day, day))
            .isInstanceOf(ConflictException.class).hasMessageContaining("Trùng phòng");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_sessions WHERE class_schedule_id=?",
            Long.class, schedule.id())).isZero();
    }

    @Test
    @DisplayName("TC-F8.7.1-03a, TC-F8.7.1-04a: Chỉ chặn lịch có ngày phát sinh chồng giờ")
    void recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges() {
        schedules.save(manager(), null, request(2, LocalDate.of(2026,11,17), LocalDate.of(2026,11,23)));
        assertThatCode(() -> schedules.save(manager(), null, request(2, LocalDate.of(2026,11,18), LocalDate.of(2026,11,24))))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> schedules.save(manager(), null, request(2, LocalDate.of(2026,11,24), null)))
            .hasMessageContaining("Trùng");
    }

    @Test
    @DisplayName("TC-F8.7.1-02a: Chặn lịch sai giờ, sức chứa hoặc vai trò người dạy")
    void validatesResourcesDatesCapacityAndRole() {
        var r = request(3, NOW.toLocalDate(), null);
        assertThatThrownBy(() -> schedules.save(actor(), null, r)).hasMessageContaining("quản lý");
        assertThatThrownBy(() -> schedules.save(manager(), null,
            new ScheduleRequest(type,trainer,room,3,r.endTime(),r.startTime(),10,r.validFrom(),null)))
            .hasMessageContaining("Giờ kết thúc");
        assertThatThrownBy(() -> schedules.save(manager(), null,
            new ScheduleRequest(type,trainer,room,3,r.startTime(),r.endTime(),21,r.validFrom(),null)))
            .hasMessageContaining("sức chứa");
        jdbc.update("UPDATE users SET role='RECEPTIONIST' WHERE id=(SELECT user_id FROM staff WHERE id=?)", trainer);
        assertThatThrownBy(() -> schedules.save(manager(), null, r)).hasMessageContaining("không hợp lệ");
    }

    @Test
    @DisplayName("TC-F8.1.1-01a, TC-F8.1.1-05b, TC-F8.1.1-09a: Đặt chính chủ, ghi người tạo, chặn đặt trùng hoặc đặt hộ")
    void apiAuthorizationValidationAndPersistence() throws Exception {
        long s = session(NOW.plusHours(3), 1);
        String body = json.writeValueAsString(new BookingRequest(member));
        mvc.perform(post("/api/v1/class-sessions/"+s+"/bookings").contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/class-sessions/"+s+"/bookings").header("Authorization",bearer(manager,"BRANCH_MANAGER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        long stranger = user("stranger","MEMBER");
        mvc.perform(post("/api/v1/class-sessions/"+s+"/bookings").header("Authorization",bearer(stranger,"MEMBER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/class-sessions/"+s+"/bookings").header("Authorization",bearer(user,"MEMBER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isCreated())
            .andExpect(header().exists("Location")).andExpect(jsonPath("$.createdBy").value(user));
        mvc.perform(post("/api/v1/class-sessions/"+s+"/bookings").header("Authorization",bearer(user,"MEMBER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/class-schedules").header("Authorization",bearer(manager,"BRANCH_MANAGER"))
            .param("size","0")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.size").exists());
        mvc.perform(post("/api/v1/class-schedules").header("Authorization",bearer(manager,"BRANCH_MANAGER"))
            .contentType(APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.capacity").exists());
    }

    @Test
    @DisplayName("TC-F8.1.1-01b, TC-F8.3.1-01b: Lễ tân đúng chi nhánh đặt và hủy hộ")
    void receptionistIsRecordedWhenBookingForMember() throws Exception {
        long session = session(NOW.plusHours(3), 1);
        mvc.perform(post("/api/v1/class-sessions/{id}/bookings", session)
            .header("Authorization", bearer(receptionist, "RECEPTIONIST")).contentType(APPLICATION_JSON)
            .content(json.writeValueAsString(new BookingRequest(member))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdBy").value(receptionist))
            .andExpect(jsonPath("$.memberId").value(member));
        long booking = jdbc.queryForObject("SELECT id FROM class_bookings WHERE class_session_id=?", Long.class, session);

        mvc.perform(put("/api/v1/class-bookings/{id}/cancel", booking)
            .header("Authorization", bearer(receptionist, "RECEPTIONIST")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cancelReason").value("BY_STAFF"))
            .andExpect(jsonPath("$.isLateCancel").value(false));
    }

    @Test
    @DisplayName("TC-F8.1.1-09b, TC-F8.3.1-08a, TC-F8.7.1-08a: Chặn thao tác ngoài chi nhánh hoặc sai chủ chỗ")
    void staffCannotCrossBranchesAndMemberCannotReadOrCancelOthersBookings() throws Exception {
        long s = session(NOW.plusHours(3), 1);
        var b = bookings.book(actor(), s, member);
        long stranger = user("outsider", "MEMBER");
        for (String path : List.of("/api/v1/class-bookings/"+b.id(), "/api/v1/class-bookings/"+b.id()+"/cancel")) {
            var request = path.endsWith("cancel") ? put(path) : get(path);
            mvc.perform(request.header("Authorization", bearer(stranger,"MEMBER"))).andExpect(status().isForbidden());
        }
        jdbc.update("UPDATE staff SET branch_id=? WHERE user_id IN (?,?)", otherBranch, manager, receptionist);
        assertThatThrownBy(() -> schedules.detail(manager(), jdbc.queryForObject(
            "SELECT class_schedule_id FROM class_sessions WHERE id=?", Long.class, s))).hasMessageContaining("ngoài chi nhánh");
        assertThatThrownBy(() -> bookings.cancel(new CurrentUser(receptionist,UserRole.RECEPTIONIST),b.id()))
            .hasMessageContaining("không có quyền");
        assertThatThrownBy(() -> bookings.book(new CurrentUser(receptionist, UserRole.RECEPTIONIST), s, member))
            .hasMessageContaining("không có quyền");
        assertThat(bookings.detail(actor(),b.id()).status()).isEqualTo("BOOKED");
    }

    @ParameterizedTest @ValueSource(strings = {"{\"memberId\":1.5}", "{\"memberId\":\"1\"}",
        "{\"memberId\":1,\"createdBy\":2}", "{\"memberId\":0}", "{}"})
    @DisplayName("S3, S5: Từ chối trường giả mạo và kiểu dữ liệu không hợp lệ")
    void invalidOrForgedBookingPayloadReturnsFieldErrors(String body) throws Exception {
        long s = session(NOW.plusHours(3),1);
        mvc.perform(post("/api/v1/class-sessions/"+s+"/bookings").header("Authorization",bearer(user,"MEMBER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isMap());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings",Long.class)).isZero();
    }

    @Test
    @DisplayName("TC-F8.3.1-07a: Hai lượt hủy đồng thời chỉ trả chỗ và ghi phạt một lần")
    void doubleCancellationOnlyRecordsOneViolation() throws Exception {
        long session = session(NOW.plusHours(1), 1);
        var booking = bookings.book(actor(), session, member);
        Callable<String> cancel = () -> {
            try {
                return bookings.cancel(actor(), booking.id()).status();
            } catch (BusinessRuleException ex) {
                return "REJECTED";
            }
        };

        assertThat(race(cancel, cancel)).containsExactlyInAnyOrder("CANCELLED", "REJECTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings WHERE is_late_cancel", Long.class))
            .isEqualTo(1);
    }

    @Test
    @DisplayName("BR-F8.3-02: Lỗi ghi nhật ký khóa hoàn tác cả hủy chỗ và mốc khóa")
    void failedPenaltyAuditRollsBackCancellation() {
        long session = session(NOW.plusHours(1), 1);
        var booking = bookings.book(actor(), session, member);
        jdbc.update("INSERT INTO system_configs(branch_id,config_key,config_value) VALUES (?,'PENALTY_LIMIT_PER_MONTH','1')", branch);
        jdbc.execute("ALTER TABLE audit_logs ADD CONSTRAINT reject_f8_lock CHECK (action <> 'CLASS_BOOKING_LOCK')");
        try {
            assertThatThrownBy(() -> bookings.cancel(actor(), booking.id()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            var current = bookings.detail(actor(), booking.id());
            assertThat(current.status()).isEqualTo("BOOKED");
            assertThat(current.bookingLockedUntil()).isNull();
            assertThat(current.isLateCancel()).isFalse();
        } finally {
            jdbc.execute("ALTER TABLE audit_logs DROP CONSTRAINT reject_f8_lock");
        }
    }

    @Test
    @DisplayName("TC-F8.1.1-08b: Dưới mốc đóng vẫn từ chối kể cả khi lớp đã đầy hoặc vừa có chỗ")
    void afterCutoffCancellationReleasesSeatWithoutReopeningBooking() {
        long session = session(NOW.plusHours(1), 1);
        var first = bookings.book(actor(), session, member);
        long secondUser = user("second", "MEMBER");
        long secondMember = member(secondUser);
        timePackage(secondMember);
        var second = new CurrentUser(secondUser, UserRole.MEMBER);
        at(NOW.plusMinutes(31));

        assertThatThrownBy(() -> bookings.book(second, session, secondMember)).hasMessageContaining("đóng đặt");
        bookings.cancel(actor(), first.id());
        assertThatThrownBy(() -> bookings.book(second, session, secondMember)).hasMessageContaining("đóng đặt");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings WHERE status='BOOKED'", Long.class)).isZero();
    }

    @Test
    @DisplayName("TC-F8.8.1-02a, TC-F8.8.1-04a: Sinh đồng thời hoặc sinh lại sau đổi giờ không tạo bản sao")
    void concurrentGenerationAndChangedSessionDoNotCreateDuplicates() throws Exception {
        var s=schedules.save(manager(),null,request(3,NOW.toLocalDate(),NOW.toLocalDate()));
        var runs=race(() -> Integer.toString(schedules.generate(s.id(),NOW.toLocalDate(),NOW.toLocalDate())),
            () -> Integer.toString(schedules.generate(s.id(),NOW.toLocalDate(),NOW.toLocalDate())));
        assertThat(runs).containsExactlyInAnyOrder("1","0");
        jdbc.update("UPDATE class_sessions SET start_at=?,end_at=?",NOW.plusHours(5),NOW.plusHours(6));
        assertThat(schedules.generate(s.id(),NOW.toLocalDate(),NOW.toLocalDate())).isZero();
        assertThat(jdbc.queryForObject("SELECT start_at FROM class_sessions",OffsetDateTime.class).toInstant())
            .isEqualTo(NOW.plusHours(5).toInstant());
    }

    @Test
    @DisplayName("TC-F8.7.1-03b: Chặn xung đột với buổi đã đổi giờ thuộc cùng lịch")
    void recurringUpdateRejectsConflictWithItsOwnRescheduledSession() {
        LocalDate wednesday = NOW.toLocalDate();
        LocalDate tuesday = wednesday.minusDays(1);
        var saved = schedules.save(manager(), null, request(2, tuesday, null));
        assertThat(schedules.generate(saved.id(), tuesday, tuesday)).isEqualTo(1);
        jdbc.update("UPDATE class_sessions SET start_at=?,end_at=?,status='RESCHEDULED' WHERE class_schedule_id=?",
            NOW.plusHours(3), NOW.plusHours(4), saved.id());
        assertThatThrownBy(() -> schedules.save(manager(), saved.id(), request(3, wednesday, null)))
            .isInstanceOf(vn.edu.hust.nhom6.gym.common.exception.ConflictException.class);
        assertThat(schedules.detail(manager(), saved.id()).dayOfWeek()).isEqualTo(2);
    }

    @Test
    @DisplayName("TC-F8.7.1-08b: Lịch lặp chặn tài nguyên ngoài phạm vi chi nhánh")
    void scheduleApiDistinguishesMissingOutOfScopeAndIneligibleResources() throws Exception {
        long otherRoom = jdbc.queryForObject("INSERT INTO rooms(branch_id,name,capacity) VALUES (?,'Khác',20) RETURNING id",
            Long.class, otherBranch);
        String body = json.writeValueAsString(request(3, NOW.toLocalDate(), null));
        String auth = bearer(manager, "BRANCH_MANAGER");
        mvc.perform(post("/api/v1/class-schedules").header("Authorization", auth)
            .contentType(APPLICATION_JSON).content(body.replace("\"roomId\":" + room, "\"roomId\":" + otherRoom)))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/class-schedules").header("Authorization", auth)
            .contentType(APPLICATION_JSON).content(body.replace("\"roomId\":" + room, "\"roomId\":999999")))
            .andExpect(status().isNotFound());
        jdbc.update("UPDATE users SET role='RECEPTIONIST' WHERE id=(SELECT user_id FROM staff WHERE id=?)", trainer);
        mvc.perform(post("/api/v1/class-schedules").header("Authorization", auth)
            .contentType(APPLICATION_JSON).content(body))
            .andExpect(status().isUnprocessableContent());
    }

    @ParameterizedTest @ValueSource(strings = {"ROOM", "TRAINER", "PT"})
    @DisplayName("TC-F8.7.1-03c: Chặn trùng phòng, huấn luyện viên hoặc buổi PT")
    void scheduleRejectsConcreteRoomTrainerAndPtConflicts(String conflict) {
        long otherTrainer = staff(user("other-trainer", "TRAINER"), "PT2");
        long otherRoom = jdbc.queryForObject("INSERT INTO rooms(branch_id,name,capacity) VALUES (?,'Khác',20) RETURNING id",
            Long.class, branch);
        if (conflict.equals("PT")) {
            long pkg = jdbc.queryForObject("""
                INSERT INTO packages(code,name,type,duration_days,session_count,price)
                VALUES ('PT-TEST','PT','PT',30,10,500000) RETURNING id
                """, Long.class);
            long membership = jdbc.queryForObject("""
                INSERT INTO memberships(member_id,package_id,package_type,sold_branch_id,start_date,end_date,
                    total_sessions,remaining_sessions,status)
                VALUES (?,?,'PT',?,'2026-11-01','2026-12-31',10,10,'ACTIVE') RETURNING id
                """, Long.class, member, pkg, branch);
            jdbc.update("""
                INSERT INTO pt_sessions(trainer_id,member_id,membership_id,branch_id,start_at,end_at)
                VALUES (?,?,?,?,?,?)
                """, trainer, member, membership, branch, NOW.plusHours(3), NOW.plusHours(4));
        } else {
            long existing = session(NOW.plusHours(3), 1);
            // Lịch gốc đã ngừng nhưng buổi đã sinh vẫn giữ phòng/huấn luyện viên.
            jdbc.update("UPDATE class_schedules SET status='INACTIVE' WHERE id=(SELECT class_schedule_id FROM class_sessions WHERE id=?)",
                existing);
        }
        var r = new ScheduleRequest(type, conflict.equals("ROOM") ? otherTrainer : trainer,
            conflict.equals("ROOM") ? room : otherRoom, 3, LocalTime.of(13,0), LocalTime.of(14,0),
            10, NOW.toLocalDate(), NOW.toLocalDate());
        assertThatThrownBy(() -> schedules.save(manager(), null, r)).hasMessageContaining("trùng");
    }

    @Test
    @DisplayName("TC-F8.7.1-01a: Tạo, đọc, cập nhật và ngừng lịch lặp qua API")
    void scheduleApiCreatesReadsUpdatesDeactivatesAndChecksNumericFields() throws Exception {
        String body=json.writeValueAsString(request(2,NOW.toLocalDate(),null));
        var response=mvc.perform(post("/api/v1/class-schedules").header("Authorization",bearer(manager,"BRANCH_MANAGER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        long id=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/v1/class-schedules/"+id).header("Authorization",bearer(manager,"BRANCH_MANAGER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.branchId").value(branch));
        mvc.perform(put("/api/v1/class-schedules/"+id).header("Authorization",bearer(manager,"BRANCH_MANAGER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/class-schedules/"+id+"/deactivate").header("Authorization",bearer(manager,"BRANCH_MANAGER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(post("/api/v1/class-schedules").header("Authorization",bearer(manager,"BRANCH_MANAGER"))
            .contentType(APPLICATION_JSON).content(body.replace("\"capacity\":10","\"capacity\":10.5")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.capacity").exists());
    }

    @Test
    @DisplayName("TC-F8.7.1-08c, TC-F8.7.1-09a, TC-F8.7.1-10a, TC-F8.7.1-11a: Đổi phòng, đổi giờ nhiều lần và hủy buổi với dữ liệu F10")
    void changesPreserveBookingsUntilCancellationAndPublishRecipientSnapshot() throws Exception {
        long id = session(NOW.plusHours(2), 2);
        long booking = bookings.book(actor(), id, member).id();
        long otherRoom = jdbc.queryForObject("INSERT INTO rooms(branch_id,name,capacity) VALUES (?,'Phòng mới',20) RETURNING id",
            Long.class, branch);
        long owner = user("owner", "SUPER_ADMIN");
        var roomUrl = "/api/v1/class-sessions/" + id + "/room";
        mvc.perform(put(roomUrl).header("Authorization", bearer(receptionist, "RECEPTIONIST"))
            .contentType(APPLICATION_JSON).content(json.writeValueAsString(new SessionRoomRequest(otherRoom))))
            .andExpect(status().isForbidden());
        mvc.perform(put(roomUrl).header("Authorization", bearer(owner, "SUPER_ADMIN"))
            .contentType(APPLICATION_JSON).content(json.writeValueAsString(new SessionRoomRequest(otherRoom))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.roomId").value(otherRoom));

        var firstStart = NOW.plusDays(1);
        var secondStart = firstStart.plusHours(2);
        sessions.changeTime(manager(), id, new SessionTimeRequest(firstStart, firstStart.plusHours(1)));
        var changed = sessions.changeTime(manager(), id, new SessionTimeRequest(secondStart, secondStart.plusHours(1)));
        assertThat(changed.originalStartAt().toInstant()).isEqualTo(NOW.plusHours(2).toInstant());
        assertThat(changed.occurrenceDate()).isEqualTo(NOW.toLocalDate());
        assertThat(bookings.detail(actor(), booking).status()).isEqualTo("BOOKED");

        at(secondStart.minusMinutes(20));
        assertThat(bookings.cancel(actor(), booking).isLateCancel()).isFalse();
        long nextId = session(secondStart.plusDays(1), 2);
        long nextBooking = bookings.book(actor(), nextId, member).id();
        sessions.cancel(manager(), nextId);
        sessions.cancel(manager(), nextId);
        assertThat(bookings.detail(actor(), nextBooking)).satisfies(cancelled -> {
            assertThat(cancelled.status()).isEqualTo("CANCELLED");
            assertThat(cancelled.cancelReason()).isEqualTo("SESSION_CANCELLED");
            assertThat(cancelled.isLateCancel()).isFalse();
        });

        var published = events.stream(ClassSessionChanged.class).toList();
        assertThat(published).extracting(ClassSessionChanged::kind)
            .containsExactly(Kind.ROOM_CHANGED, Kind.TIME_CHANGED, Kind.TIME_CHANGED, Kind.CANCELLED);
        assertThat(published).allSatisfy(event -> assertThat(event.affectedMemberIds()).containsExactly(member));
        assertThat(published.get(0).before().roomId()).isEqualTo(room);
        assertThat(published.get(2).before().startAt().toInstant()).isEqualTo(firstStart.toInstant());
        assertThat(published.get(2).after().startAt().toInstant()).isEqualTo(secondStart.toInstant());
    }

    @Test
    @DisplayName("TC-F8.7.1-09b, TC-F8.7.1-10b: Giữ nguyên buổi khi xung đột hoặc phòng không hợp lệ")
    void rejectsRoomCapacityAndActualTimeConflict() {
        long id = session(NOW.plusHours(2), 2);
        session(NOW.plusHours(4), 2);
        long smallRoom = jdbc.queryForObject("INSERT INTO rooms(branch_id,name,capacity) VALUES (?,'Phòng nhỏ',1) RETURNING id",
            Long.class, branch);
        long foreignRoom = jdbc.queryForObject("INSERT INTO rooms(branch_id,name,capacity) VALUES (?,'Ngoài chi nhánh',20) RETURNING id",
            Long.class, otherBranch);
        assertThatThrownBy(() -> sessions.changeRoom(manager(), id, smallRoom)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> sessions.changeRoom(manager(), id, foreignRoom))
            .isInstanceOf(vn.edu.hust.nhom6.gym.common.exception.ForbiddenException.class);
        assertThatThrownBy(() -> sessions.changeTime(manager(), id,
            new SessionTimeRequest(NOW.plusHours(4), NOW.plusHours(5)))).isInstanceOf(ConflictException.class);
        assertThat(events.stream(ClassSessionChanged.class)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT room_id FROM class_sessions WHERE id=?", Long.class, id)).isEqualTo(room);
    }

    @Test
    @DisplayName("TC-F8.7.1-12a: Rollback không phát sự kiện, lỗi thông báo không đảo ngược F8")
    void notificationFailureIsIsolatedAndRollbackDoesNotPublish() {
        long id = session(NOW.plusHours(2), 2);
        long booking = bookings.book(actor(), id, member).id();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            sessions.cancel(manager(), id);
            tx.setRollbackOnly();
        });
        assertThat(events.stream(ClassSessionChanged.class)).isEmpty();
        assertThat(bookings.detail(actor(), booking).status()).isEqualTo("BOOKED");

        notifications.fail = true;
        try {
            assertThat(sessions.cancel(manager(), id).status()).isEqualTo("CANCELLED");
        } finally {
            notifications.fail = false;
        }
        assertThat(notifications.observedStatus).isEqualTo("CANCELLED");
        assertThat(bookings.detail(actor(), booking).status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("TC-F8.6.1-01a, TC-F8.6.1-03a, TC-F8.6.1-04a, TC-F8.6.1-05a, TC-F8.6.1-06a: Check-in, phân công, biên giờ Việt Nam và audit khi sửa")
    void attendanceHonorsAssignmentAndVietnameseDay() throws Exception {
        long id = session(NOW.plusHours(2), 2);
        long booking = bookings.book(actor(), id, member).id();
        jdbc.update("UPDATE class_bookings SET arrived_at=? WHERE id=?", NOW, booking);
        String path = "/api/v1/class-sessions/" + id + "/attendance";
        long otherTrainer = user("other-trainer", "TRAINER");
        staff(otherTrainer, "PT2");
        String trainerJwt = bearer(trainerUser, "TRAINER");
        mvc.perform(get(path).header("Authorization", bearer(otherTrainer, "TRAINER")))
            .andExpect(status().isForbidden());
        mvc.perform(get(path).header("Authorization", trainerJwt)).andExpect(status().isOk())
            .andExpect(jsonPath("$.bookings[0].status").value("BOOKED"))
            .andExpect(jsonPath("$.bookings[0].arrivedAt").exists());

        var trainerActor = new CurrentUser(trainerUser, UserRole.TRAINER);
        var original = attendance.roster(trainerActor, id).bookings().getFirst();
        var mark = new AttendanceRequest(booking, AttendanceStatus.ATTENDED, original.updatedAt());
        assertThatThrownBy(() -> attendance.mark(trainerActor, id, mark)).isInstanceOf(BusinessRuleException.class);
        at(NOW.plusHours(2));
        mvc.perform(put(path).header("Authorization", trainerJwt).contentType(APPLICATION_JSON)
            .content(json.writeValueAsString(mark))).andExpect(status().isOk())
            .andExpect(jsonPath("$.attendanceMarkedBy").value(trainerUser));
        assertThatThrownBy(() -> attendance.mark(trainerActor, id, mark)).isInstanceOf(ConflictException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM class_bookings WHERE id=?", String.class, booking))
            .isEqualTo("ATTENDED");

        at(NOW.withHour(23).withMinute(59));
        var marked = attendance.roster(trainerActor, id).bookings().getFirst();
        var corrected = attendance.mark(trainerActor, id, new AttendanceRequest(booking, AttendanceStatus.NO_SHOW, marked.updatedAt()));
        assertThat(corrected.status()).isEqualTo("NO_SHOW");
        var correct = new AttendanceRequest(booking, AttendanceStatus.ATTENDED, corrected.updatedAt());
        assertThatThrownBy(() -> attendance.mark(manager(), id, correct)).isInstanceOf(BusinessRuleException.class);
        at(NOW.plusDays(1).withHour(0));
        assertThatThrownBy(() -> attendance.mark(trainerActor, id, correct)).isInstanceOf(BusinessRuleException.class);
        assertThat(attendance.mark(manager(), id, correct).attendanceMarkedBy()).isEqualTo(manager);
        assertThat(jdbc.queryForObject("""
            SELECT (old_value->>'status') || ':' || (new_value->>'status')
            FROM audit_logs WHERE action='CLASS_ATTENDANCE_CORRECT' AND user_id=? AND entity_id=?
            """, String.class, manager, booking)).isEqualTo("NO_SHOW:ATTENDED");
    }

    @Test
    @DisplayName("TC-F8.6.1-02a, TC-F8.6.1-07a, TC-F8.6.1-08a, TC-F8.6.1-09a, TC-F8.6.1-10a, TC-F8.6.1-11a: Chỉ NO_SHOW lần đầu áp khóa, gỡ khóa riêng có audit")
    void firstNoShowLocksAndCorrectionDoesNotReevaluateLock() throws Exception {
        var trainerActor = new CurrentUser(trainerUser, UserRole.TRAINER);
        jdbc.update("INSERT INTO system_configs(branch_id,config_key,config_value) VALUES (?,'PENALTY_LIMIT_PER_MONTH','1')", branch);
        long id = session(NOW.plusHours(2), 2);
        long booking = bookings.book(actor(), id, member).id();
        at(NOW.plusHours(2));
        var row = attendance.roster(trainerActor, id).bookings().getFirst();
        var marked = attendance.mark(trainerActor, id, new AttendanceRequest(booking, AttendanceStatus.NO_SHOW, row.updatedAt()));
        var lockedUntil = bookings.detail(actor(), booking).bookingLockedUntil();
        assertThat(lockedUntil).isNotNull();
        var corrected = attendance.mark(trainerActor, id,
            new AttendanceRequest(booking, AttendanceStatus.ATTENDED, marked.updatedAt()));
        assertThat(bookings.detail(actor(), booking).bookingLockedUntil()).isEqualTo(lockedUntil);
        attendance.mark(trainerActor, id, new AttendanceRequest(booking, AttendanceStatus.NO_SHOW, corrected.updatedAt()));
        assertThat(bookings.detail(actor(), booking).bookingLockedUntil()).isEqualTo(lockedUntil);

        String path = "/api/v1/members/" + member + "/class-booking-lock";
        long foreignManager = user("foreign-manager", "BRANCH_MANAGER");
        jdbc.update("INSERT INTO staff(user_id,branch_id,staff_code,full_name) VALUES (?,?,'Q2','QL2')", foreignManager, otherBranch);
        mvc.perform(delete(path).header("Authorization", bearer(foreignManager, "BRANCH_MANAGER")))
            .andExpect(status().isForbidden());
        mvc.perform(delete(path).header("Authorization", bearer(trainerUser, "TRAINER"))).andExpect(status().isForbidden());
        mvc.perform(delete(path).header("Authorization", bearer(manager, "BRANCH_MANAGER"))).andExpect(status().isNoContent());
        assertThat(bookings.detail(actor(), booking).bookingLockedUntil()).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE entity='members' AND entity_id=? AND user_id=? AND action='CLASS_BOOKING_UNLOCK'",
            Long.class, member, manager)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE entity='members' AND entity_id=? AND action='CLASS_BOOKING_LOCK'",
            Long.class, member)).isEqualTo(1L);

        long next = session(NOW.plusDays(1), 2);
        long cancelled = bookings.book(actor(), next, member).id();
        var open = attendance.roster(trainerActor, next).bookings().getFirst();
        bookings.cancel(actor(), cancelled);
        at(NOW.plusDays(1));
        assertThatThrownBy(() -> attendance.mark(trainerActor, next,
            new AttendanceRequest(cancelled, AttendanceStatus.NO_SHOW, open.updatedAt())))
            .isInstanceOf(BusinessRuleException.class);
        assertThat(bookings.detail(actor(), cancelled).bookingLockedUntil()).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EventTestConfiguration {
        @Bean
        FailingNotificationListener failingNotificationListener(JdbcTemplate jdbc) {
            return new FailingNotificationListener(jdbc);
        }
    }

    static class FailingNotificationListener {
        private final JdbcTemplate jdbc;
        boolean fail;
        String observedStatus;

        FailingNotificationListener(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @EventListener
        void receive(ClassSessionChanged event) {
            if (fail) {
                observedStatus = jdbc.queryForObject("SELECT status FROM class_sessions WHERE id=?", String.class, event.sessionId());
                throw new IllegalStateException("Giả lập lỗi xử lý thông báo");
            }
        }
    }
}
