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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.TestcontainersConfiguration;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.auth.service.JwtTokenService;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.*;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberActor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** PostgreSQL/Flyway thật; chỉ điều khiển đồng hồ, không mock nghiệp vụ hoặc khóa DB. */
@SpringBootTest(properties = "app.classes.generation.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClassApiIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JwtTokenService tokens;
    @Autowired ClassBookingService bookings;
    @Autowired ClassScheduleService schedules;
    @MockitoBean(name = "classClock") Clock clock;
    static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-18T10:00:00+07:00");
    long branch, otherBranch, manager, receptionist, trainer, room, type, member, user;

    @BeforeEach
    void fixture() throws Exception {
        // Chỉ database Testcontainers mới, không dùng DB local hoặc staging.
        jdbc.execute("TRUNCATE users,packages,class_types,rooms RESTART IDENTITY CASCADE");
        var configSql = new String(new org.springframework.core.io.ClassPathResource(
            "db/migration/V1_06__create_system_configs.sql").getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        jdbc.execute(configSql.substring(configSql.indexOf("INSERT INTO system_configs")));
        branch = jdbc.queryForObject("SELECT id FROM branches WHERE code='CN01'", Long.class);
        jdbc.update("INSERT INTO branches(code,name,open_time,close_time) VALUES ('F8TEST','Khác','06:00','22:00') ON CONFLICT DO NOTHING");
        otherBranch = jdbc.queryForObject("SELECT id FROM branches WHERE code='F8TEST'", Long.class);
        jdbc.update("DELETE FROM system_configs WHERE branch_id IS NOT NULL");
        manager = user("manager", "BRANCH_MANAGER");
        receptionist = user("reception", "RECEPTIONIST");
        long trainerUser = user("trainer", "TRAINER");
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

    private MemberActor actor() { return new MemberActor(user, UserRole.MEMBER); }
    private MemberActor manager() { return new MemberActor(manager, UserRole.BRANCH_MANAGER); }
    private String bearer(long id, String role) { return "Bearer " + tokens.generateAccessToken(id, "test@f8.example", UserRole.valueOf(role)); }

    @Test @DisplayName("Đặt, vào chờ, FIFO, ân hạn và thông báo cùng giao dịch")
    void bookingWaitlistCancellationAndPromotion() throws Exception {
        long s = session(NOW.plusHours(3), 1);
        var first = bookings.book(actor(), s, member);
        long u2 = user("second", "MEMBER"); long m2 = member(u2); timePackage(m2);
        var second = bookings.book(new MemberActor(u2, UserRole.MEMBER), s, m2);
        assertThat(second.status()).isEqualTo("WAITLIST"); assertThat(second.waitlistPosition()).isEqualTo(1);
        assertThat(second.createdBy()).isEqualTo(u2);
        bookings.cancel(actor(), first.id());
        var promoted = bookings.detail(new MemberActor(u2, UserRole.MEMBER), second.id());
        assertThat(promoted.status()).isEqualTo("BOOKED"); assertThat(promoted.createdBy()).isEqualTo(u2);
        assertThat(promoted.promotedAt().toInstant()).isEqualTo(NOW.toInstant());
        assertThat(promoted.graceUntil().toInstant()).isEqualTo(NOW.plusMinutes(15).toInstant());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE related_id=?", Long.class, second.id())).isEqualTo(2);
        assertThat(bookings.book(actor(), s, member).status()).isEqualTo("WAITLIST");
    }

    @ParameterizedTest @ValueSource(strings = {"SESSION", "PT", "EXPIRED", "PENDING", "FROZEN", "WRONG_BRANCH", "ENDED", "LOCKED", "INACTIVE"})
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
        assertThatThrownBy(() -> bookings.book(actor(), s, member)).hasMessageContaining("TIME");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings", Long.class)).isZero();
    }

    @Test
    void dayWindowUsesVietnamDatesAndBranchOverrides() {
        long lastDay = session(NOW.plusDays(7).withHour(23), 1);
        assertThat(bookings.book(actor(), lastDay, member).status()).isEqualTo("BOOKED");
        long outside = session(NOW.plusDays(8), 1);
        assertThatThrownBy(() -> bookings.book(actor(), outside, member)).hasMessageContaining("cửa sổ");
        jdbc.update("INSERT INTO system_configs(branch_id,config_key,config_value) VALUES (?,'BOOKING_WINDOW_DAYS','8')", branch);
        assertThat(bookings.book(actor(), outside, member).status()).isEqualTo("BOOKED");
    }

    @ParameterizedTest @CsvSource({"30,true", "29,false"})
    void cutoffBoundary(long minutes, boolean permitted) {
        long s = session(NOW.plusMinutes(minutes), 1);
        if (permitted) { assertThat(bookings.book(actor(), s, member).status()).isEqualTo("BOOKED"); }
        else { assertThatThrownBy(() -> bookings.book(actor(), s, member)).hasMessageContaining("đóng đặt"); }
    }

    @ParameterizedTest @CsvSource({"120,false", "119,true"})
    void lateCancellationBoundary(long minutes, boolean late) {
        long s = session(NOW.plusMinutes(minutes), 1);
        var b = bookings.book(actor(), s, member);
        assertThat(bookings.cancel(actor(), b.id()).isLateCancel()).isEqualTo(late);
        assertThatThrownBy(() -> bookings.cancel(actor(), b.id())).hasMessageContaining("không thể hủy");
    }

    @ParameterizedTest @CsvSource({"0,false", "1,true"})
    void promotedGraceBoundary(int secondsAfter, boolean late) {
        long s = session(NOW.plusHours(1), 1);
        var b = bookings.book(actor(), s, member);
        jdbc.update("UPDATE class_bookings SET promoted_at=?,grace_until=? WHERE id=?", NOW.minusMinutes(15), NOW, b.id());
        at(NOW.plusSeconds(secondsAfter));
        assertThat(bookings.cancel(actor(), b.id()).isLateCancel()).isEqualTo(late);
    }

    @Test
    void waitlistLeavingOneMinuteBeforeClassNeverPenalizes() {
        long s = session(NOW.plusHours(1), 1);
        bookings.book(actor(), s, member);
        long u2 = user("wait", "MEMBER"); long m2 = member(u2); timePackage(m2);
        var a2 = new MemberActor(u2, UserRole.MEMBER);
        var b = bookings.book(a2, s, m2);
        at(NOW.plusMinutes(59));
        assertThat(bookings.cancel(a2, b.id()).isLateCancel()).isFalse();
    }

    @Test
    void violationsLockOnceAndExpiredLockDoesNotReuseOldViolations() {
        for (int i = 0; i < 3; i++) {
            long s = session(NOW.plusMinutes(60 + i * 61), 1);
            var b = bookings.book(actor(), s, member);
            // Các buổi không giao nhau; hủy muộn tại thời điểm phù hợp của từng buổi.
            at(NOW.plusMinutes(i * 61)); bookings.cancel(actor(), b.id());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action='CLASS_BOOKING_LOCK'", Long.class)).isEqualTo(1);
        at(NOW.plusDays(8));
        long s = session(NOW.plusDays(8).plusHours(1), 1);
        var b = bookings.book(actor(), s, member);
        assertThat(bookings.cancel(actor(), b.id()).isLateCancel()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE action='CLASS_BOOKING_LOCK'", Long.class)).isEqualTo(1);
    }

    @Test
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
    void skipsIneligibleWaitersButPreservesPositionAndCreator() {
        long s = session(NOW.plusHours(3), 1);
        var first = bookings.book(actor(), s, member);
        long u2 = user("second", "MEMBER"); long m2 = member(u2); timePackage(m2);
        long u3 = user("third", "MEMBER"); long m3 = member(u3); timePackage(m3);
        var waiter = bookings.book(new MemberActor(u2, UserRole.MEMBER), s, m2);
        var next = bookings.book(new MemberActor(receptionist, UserRole.RECEPTIONIST), s, m3);
        jdbc.update("UPDATE memberships SET status='EXPIRED' WHERE member_id=?", m2);
        bookings.cancel(actor(), first.id());
        assertThat(bookings.detail(new MemberActor(u2, UserRole.MEMBER), waiter.id()).status()).isEqualTo("WAITLIST");
        assertThat(bookings.detail(new MemberActor(u3, UserRole.MEMBER), next.id()).createdBy()).isEqualTo(receptionist);
        assertThat(bookings.detail(new MemberActor(u3, UserRole.MEMBER), next.id()).status()).isEqualTo("BOOKED");
    }

    @Test
    void returnedWaiterIsPromotedBeforeNewcomerAndAnonymousProfileGetsGrace() {
        long s = session(NOW.plusHours(3), 1);
        var first = bookings.book(actor(), s, member);
        long m2 = member(null); timePackage(m2);
        var front = new MemberActor(receptionist, UserRole.RECEPTIONIST);
        var wait = bookings.book(front, s, m2);
        jdbc.update("UPDATE memberships SET status='EXPIRED' WHERE member_id=?", m2);
        bookings.cancel(actor(), first.id());
        jdbc.update("UPDATE memberships SET status='ACTIVE' WHERE member_id=?", m2);
        assertThat(bookings.book(actor(), s, member).status()).isEqualTo("WAITLIST");
        assertThat(bookings.detail(front, wait.id()).graceUntil()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE related_id=?", Long.class, wait.id())).isZero();
    }

    @Test
    void concurrentLastSeatAndOverlappingRequestsRespectCapacityAndMemberLock() throws Exception {
        long s = session(NOW.plusHours(3), 1);
        long u2 = user("racer", "MEMBER"); long m2 = member(u2); timePackage(m2);
        var result = race(() -> bookings.book(actor(), s, member).status(),
            () -> bookings.book(new MemberActor(u2, UserRole.MEMBER), s, m2).status());
        assertThat(result).containsExactlyInAnyOrder("BOOKED", "WAITLIST");
        long left = session(NOW.plusDays(1), 1), right = session(NOW.plusDays(1).plusMinutes(30), 1);
        assertThat(race(() -> attemptBook(left), () -> attemptBook(right)))
            .containsExactlyInAnyOrder("BOOKED", "CONFLICT");
    }

    private String attemptBook(long s) {
        try { return bookings.book(actor(), s, member).status(); }
        catch (vn.edu.hust.nhom6.gym.common.exception.ConflictException ex) { return "CONFLICT"; }
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
    void scheduleGenerationIsIdempotentSnapshotsAndDeactivationPreserveSessions() {
        var r = request(2, NOW.toLocalDate(), NOW.toLocalDate().plusDays(14));
        var s = schedules.save(manager(), null, r);
        assertThat(schedules.generate(s.id(), NOW.toLocalDate(), NOW.toLocalDate().plusDays(14))).isEqualTo(2);
        assertThat(schedules.generate(s.id(), NOW.toLocalDate(), NOW.toLocalDate().plusDays(14))).isZero();
        assertThat(jdbc.queryForObject("SELECT min(start_at)::text FROM class_sessions", String.class)).contains("06:00:00");
        var changed = new ScheduleRequest(type, trainer, room, 2, LocalTime.of(15,0), LocalTime.of(16,0), 5, r.validFrom(), r.validTo());
        schedules.save(manager(), s.id(), changed);
        assertThat(jdbc.queryForObject("SELECT min(capacity) FROM class_sessions", Integer.class)).isEqualTo(10);
        schedules.deactivate(manager(), s.id());
        assertThat(schedules.generate(s.id(), NOW.toLocalDate(), NOW.toLocalDate().plusDays(14))).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_sessions", Long.class)).isEqualTo(2);
    }

    @Test
    void recurringOverlapUsesActualWeekdaysNotJustIntersectingDateRanges() {
        schedules.save(manager(), null, request(2, LocalDate.of(2026,11,17), LocalDate.of(2026,11,23)));
        assertThatCode(() -> schedules.save(manager(), null, request(2, LocalDate.of(2026,11,18), LocalDate.of(2026,11,24))))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> schedules.save(manager(), null, request(2, LocalDate.of(2026,11,24), null)))
            .hasMessageContaining("Trùng");
    }

    @Test
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
        assertThatThrownBy(() -> bookings.cancel(new MemberActor(receptionist,UserRole.RECEPTIONIST),b.id()))
            .hasMessageContaining("không có quyền");
        assertThat(bookings.detail(actor(),b.id()).status()).isEqualTo("BOOKED");
    }

    @ParameterizedTest @ValueSource(strings = {"{\"memberId\":1.5}", "{\"memberId\":\"1\"}",
        "{\"memberId\":1,\"createdBy\":2}", "{\"memberId\":0}", "{}"})
    void invalidOrForgedBookingPayloadReturnsFieldErrors(String body) throws Exception {
        long s = session(NOW.plusHours(3),1);
        mvc.perform(post("/api/v1/class-sessions/"+s+"/bookings").header("Authorization",bearer(user,"MEMBER"))
            .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isMap());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM class_bookings",Long.class)).isZero();
    }

    @Test
    void doubleCancellationOnlyPromotesOnce() throws Exception {
        long s = session(NOW.plusHours(3),1);
        var first = bookings.book(actor(),s,member);
        long u2=user("waiter","MEMBER"),m2=member(u2); timePackage(m2);
        var waiter=bookings.book(new MemberActor(u2,UserRole.MEMBER),s,m2);
        Callable<String> cancel = () -> {
            try { bookings.cancel(actor(),first.id()); return "CANCELLED"; }
            catch (vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException ex) { return "REJECTED"; }
        };
        assertThat(race(cancel,cancel)).containsExactlyInAnyOrder("CANCELLED","REJECTED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications WHERE related_id=?",Long.class,waiter.id())).isEqualTo(2);
    }

    @Test
    void failedNotificationRollsBackCancellationAndPromotion() {
        long s = session(NOW.plusHours(3),1);
        var first = bookings.book(actor(),s,member);
        long u2=user("waiter","MEMBER"),m2=member(u2); timePackage(m2);
        var second=bookings.book(new MemberActor(u2,UserRole.MEMBER),s,m2);
        jdbc.execute("""
            CREATE FUNCTION reject_class_notice() RETURNS trigger LANGUAGE plpgsql AS $$
            BEGIN RAISE EXCEPTION 'test notification failure'; END $$
            """);
        jdbc.execute("CREATE TRIGGER reject_class_notice BEFORE INSERT ON notifications FOR EACH ROW EXECUTE FUNCTION reject_class_notice()");
        try {
            assertThatThrownBy(() -> bookings.cancel(actor(),first.id())).isInstanceOf(RuntimeException.class);
            assertThat(bookings.detail(actor(),first.id()).status()).isEqualTo("BOOKED");
            assertThat(bookings.detail(new MemberActor(u2,UserRole.MEMBER),second.id()).status()).isEqualTo("WAITLIST");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications",Long.class)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER reject_class_notice ON notifications");
            jdbc.execute("DROP FUNCTION reject_class_notice()");
        }
    }

    @Test
    void equalWaitlistTimestampUsesIdAndNightEmailWaitsForOpening() {
        at(NOW.withHour(1));
        long s = session(NOW.withHour(9),1);
        var first = bookings.book(actor(),s,member);
        long u2=user("second","MEMBER"),m2=member(u2);timePackage(m2);
        long u3=user("third","MEMBER"),m3=member(u3);timePackage(m3);
        var second=bookings.book(new MemberActor(u2,UserRole.MEMBER),s,m2);
        var third=bookings.book(new MemberActor(u3,UserRole.MEMBER),s,m3);
        jdbc.update("UPDATE class_bookings SET waitlisted_at=? WHERE status='WAITLIST'",NOW.withHour(1));
        bookings.cancel(actor(),first.id());
        var promoted=bookings.detail(new MemberActor(u2,UserRole.MEMBER),second.id());
        assertThat(promoted.status()).isEqualTo("BOOKED");
        assertThat(promoted.graceUntil().toInstant()).isEqualTo(NOW.withHour(1).plusMinutes(15).toInstant());
        assertThat(bookings.detail(new MemberActor(u3,UserRole.MEMBER),third.id()).waitlistPosition()).isEqualTo(1);
        var sent=jdbc.queryForObject("SELECT scheduled_at FROM notifications WHERE channel='EMAIL' AND related_id=?",
            OffsetDateTime.class,second.id());
        assertThat(sent.toInstant()).isEqualTo(NOW.withHour(5).withMinute(30).toInstant());
    }

    @Test
    void afterCutoffCancellationLeavesSeatEmpty() {
        long s=session(NOW.plusHours(1),1);
        var first=bookings.book(actor(),s,member);
        long u2=user("waiter","MEMBER"),m2=member(u2);timePackage(m2);
        var second=bookings.book(new MemberActor(u2,UserRole.MEMBER),s,m2);
        at(NOW.plusMinutes(31));
        bookings.cancel(actor(),first.id());
        assertThat(bookings.detail(new MemberActor(u2,UserRole.MEMBER),second.id()).status()).isEqualTo("WAITLIST");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notifications",Long.class)).isZero();
    }

    @Test
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
}
