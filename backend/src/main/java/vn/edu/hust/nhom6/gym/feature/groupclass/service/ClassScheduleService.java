package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassScheduleService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ClassRepository classes;
    private final StaffService staff;
    private final VnTime time;
    @Value("${app.classes.generation.days:14}") private int generationDays;

    public long managerBranch(CurrentUser actor) {
        if (actor.role() != UserRole.BRANCH_MANAGER) { throw new ForbiddenException("Chỉ quản lý chi nhánh được quản lý lịch lặp."); }
        return staff.requireBranchId(actor.userId());
    }

    @Transactional(readOnly = true)
    public PageResponse<Schedule> list(CurrentUser actor, int page, int size) {
        long branch = managerBranch(actor);
        long total = classes.scheduleCount(branch);
        return new PageResponse<>(classes.schedules(branch, page, size), page, size, total,
            (int) ((total + size - 1) / size));
    }

    public Schedule save(CurrentUser actor, Long id, ScheduleRequest r) {
        long branch = managerBranch(actor);
        classes.lockBranch(branch);
        if (id != null) {
            var old = classes.schedule(id);
            if (!old.branchId().equals(branch)) { throw new ForbiddenException("Lịch lớp nằm ngoài chi nhánh quản lý."); }
            if (!old.status().equals("ACTIVE")) { throw new BusinessRuleException("Lịch đã ngừng hoạt động."); }
        }
        validate(branch, r);
        for (var other : classes.activeSchedules(branch)) {
            if (Objects.equals(id, other.id()) || !other.dayOfWeek().equals(r.dayOfWeek())
                    || (!other.roomId().equals(r.roomId()) && !other.trainerId().equals(r.trainerId()))
                    || !r.startTime().isBefore(other.endTime()) || !r.endTime().isAfter(other.startTime())) { continue; }
            LocalDate from = r.validFrom().isAfter(other.validFrom()) ? r.validFrom() : other.validFrom();
            LocalDate to = r.validTo() == null ? other.validTo() : other.validTo() == null ? r.validTo()
                : r.validTo().isBefore(other.validTo()) ? r.validTo() : other.validTo();
            LocalDate first = from.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(r.dayOfWeek())));
            if (to == null || !first.isAfter(to)) {
                throw new ConflictException("Trùng phòng hoặc huấn luyện viên với lịch lớp " + other.id() + ".");
            }
        }
        if (classes.recurringResourceConflict(id, r)) {
            throw new ConflictException("Lịch lặp trùng phòng hoặc huấn luyện viên với buổi lớp/PT đã có.");
        }
        return classes.saveSchedule(id, branch, r);
    }

    public Schedule deactivate(CurrentUser actor, long id) {
        long branch = managerBranch(actor);
        classes.lockBranch(branch);
        if (!classes.schedule(id).branchId().equals(branch)) { throw new ForbiddenException("Lịch lớp nằm ngoài chi nhánh quản lý."); }
        classes.deactivate(id);
        return classes.schedule(id);
    }

    @Transactional(readOnly = true)
    public Schedule detail(CurrentUser actor, long id) {
        var s = classes.schedule(id);
        if (!s.branchId().equals(managerBranch(actor))) { throw new ForbiddenException("Lịch lớp nằm ngoài chi nhánh quản lý."); }
        return s;
    }

    private void validate(long branch, ScheduleRequest r) {
        if (!r.endTime().isAfter(r.startTime()) || (r.validTo() != null && r.validTo().isBefore(r.validFrom()))) {
            throw new BusinessRuleException("Giờ kết thúc phải sau giờ bắt đầu và ngày kết thúc không trước ngày hiệu lực.");
        }
        var resources = classes.resources(branch, r);
        if (resources.roomBranchId() != branch || resources.trainerBranchId() != branch) {
            throw new ForbiddenException("Phòng hoặc huấn luyện viên nằm ngoài chi nhánh quản lý.");
        }
        if (!resources.active() || !resources.trainerRole()) {
            throw new BusinessRuleException("Phòng, loại lớp hoặc huấn luyện viên không hợp lệ tại chi nhánh.");
        }
        if (r.capacity() > resources.roomCapacity()) { throw new BusinessRuleException("Sĩ số vượt sức chứa phòng."); }
    }

    /** Một lịch/giao dịch: lỗi của lịch khác được job ghi log và thử lại ở lượt kế tiếp. */
    public int generate(long id, LocalDate from, LocalDate to) {
        var observed = classes.schedule(id);
        classes.lockBranch(observed.branchId());
        var s = classes.schedule(id);
        if (!s.status().equals("ACTIVE")) { return 0; }
        if (to.isBefore(from) || to.isAfter(from.plusDays(90))) {
            throw new IllegalArgumentException("Khoảng sinh buổi phải từ 0 đến 90 ngày.");
        }
        var request = new ScheduleRequest(s.classTypeId(), s.trainerId(), s.roomId(), s.dayOfWeek(),
            s.startTime(), s.endTime(), s.capacity(), s.validFrom(), s.validTo());
        validate(s.branchId(), request);
        LocalDate first = (from.isAfter(s.validFrom()) ? from : s.validFrom())
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.of(s.dayOfWeek())));
        LocalDate last = s.validTo() != null && s.validTo().isBefore(to) ? s.validTo() : to;
        int inserted = 0;
        for (LocalDate date = first; !date.isAfter(last); date = date.plusWeeks(1)) {
            if (classes.occurrenceExists(id, date)) { continue; }
            var start = time.at(date, s.startTime());
            var end = time.at(date, s.endTime());
            if (classes.resourceConflict(s.trainerId(), s.roomId(), start, end)) {
                throw new ConflictException("Trùng phòng hoặc huấn luyện viên khi sinh buổi ngày " + date + ".");
            }
            inserted += classes.insertSession(s, date, start, end);
        }
        return inserted;
    }

    /**
     * Giờ dạy lớp nhóm của huấn luyện viên {@code trainerId} chồng với {@code [start, end)} (F9 đặt buổi PT, BR-F9.3):
     * buổi lớp chưa hủy được phân công cho người này, hoặc lần lặp của lịch ACTIVE ngoài cửa sổ sinh buổi
     * ({@code today + generationDays} trở đi) chưa sinh buổi cho ngày đó.
     */
    @Transactional(readOnly = true)
    public Optional<String> findTrainerTeachingConflict(Long trainerId, OffsetDateTime start, OffsetDateTime end) {
        if (trainerId == null || start == null || end == null || !end.isAfter(start)) {
            return Optional.empty();
        }
        Stream<String> withSessions = classes.rows("""
            SELECT * FROM class_sessions
            WHERE status<>'CANCELLED' AND trainer_id=:trainer AND start_at<:end AND end_at>:start
            ORDER BY start_at LIMIT 1
            """, Map.of("trainer", trainerId, "start", start, "end", end), Session.class).stream()
            .map(other -> "Trùng huấn luyện viên với buổi #%d (%s).".formatted(other.id(), span(other.startAt(), other.endAt())));
        LocalDate firstUngenerated = time.today().plusDays(generationDays);
        Stream<String> withSchedules = time.toVnDate(start).datesUntil(time.toVnDate(end).plusDays(1))
            .filter(day -> !day.isBefore(firstUngenerated))
            .flatMap(day -> classes.rows("""
                SELECT * FROM class_schedules
                WHERE status='ACTIVE' AND trainer_id=:trainer AND day_of_week=:day
                ORDER BY id
                """, Map.of("trainer", trainerId, "day", day.getDayOfWeek().getValue()), Schedule.class).stream()
                .filter(other -> occursOn(other, day))
                .filter(other -> time.at(day, other.startTime()).isBefore(end)
                    && start.isBefore(time.at(day, other.endTime())))
                .filter(other -> !classes.occurrenceExists(other.id(), day))
                .map(other -> "Trùng huấn luyện viên với lịch #%d (%s %s–%s).".formatted(
                    other.id(), dayName(other.dayOfWeek()), other.startTime().format(TIME), other.endTime().format(TIME))));
        return Stream.of(withSessions, withSchedules).flatMap(s -> s).findFirst();
    }

    private static boolean occursOn(Schedule s, LocalDate day) {
        return !day.isBefore(s.validFrom()) && (s.validTo() == null || !day.isAfter(s.validTo()));
    }

    private static String dayName(int isoDayOfWeek) {
        return isoDayOfWeek == 7 ? "Chủ nhật" : "thứ " + (isoDayOfWeek + 1);
    }

    private static String span(OffsetDateTime start, OffsetDateTime end) {
        var vnStart = start.atZoneSameInstant(VnTime.ZONE);
        var vnEnd = end.atZoneSameInstant(VnTime.ZONE);
        return vnStart.format(DATE) + " " + vnStart.format(TIME) + "–" + vnEnd.format(TIME);
    }
}
