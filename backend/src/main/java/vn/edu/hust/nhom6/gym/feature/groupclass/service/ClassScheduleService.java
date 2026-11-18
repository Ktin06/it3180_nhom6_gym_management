package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberActor;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffLookupService;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassScheduleService {
    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ClassRepository classes;
    private final StaffLookupService staff;

    public long managerBranch(MemberActor actor) {
        if (actor.role() != UserRole.BRANCH_MANAGER) { throw new ForbiddenException("Chỉ quản lý chi nhánh được quản lý lịch lặp."); }
        return staff.activeBranchIdForUser(actor.userId());
    }

    @Transactional(readOnly = true)
    public PageResponse<Schedule> list(MemberActor actor, int page, int size) {
        long branch = managerBranch(actor);
        long total = classes.scheduleCount(branch);
        return new PageResponse<>(classes.schedules(branch, page, size), page, size, total,
            (int) ((total + size - 1) / size));
    }

    public Schedule save(MemberActor actor, Long id, ScheduleRequest r) {
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

    public Schedule deactivate(MemberActor actor, long id) {
        long branch = managerBranch(actor);
        classes.lockBranch(branch);
        if (!classes.schedule(id).branchId().equals(branch)) { throw new ForbiddenException("Lịch lớp nằm ngoài chi nhánh quản lý."); }
        classes.deactivate(id);
        return classes.schedule(id);
    }

    @Transactional(readOnly = true)
    public Schedule detail(MemberActor actor, long id) {
        var s = classes.schedule(id);
        if (!s.branchId().equals(managerBranch(actor))) { throw new ForbiddenException("Lịch lớp nằm ngoài chi nhánh quản lý."); }
        return s;
    }

    private void validate(long branch, ScheduleRequest r) {
        if (!r.endTime().isAfter(r.startTime()) || (r.validTo() != null && r.validTo().isBefore(r.validFrom()))) {
            throw new BusinessRuleException("Giờ kết thúc phải sau giờ bắt đầu và ngày kết thúc không trước ngày hiệu lực.");
        }
        var resources = classes.resources(branch, r);
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
            var start = date.atTime(s.startTime()).atZone(VIETNAM).toOffsetDateTime();
            var end = date.atTime(s.endTime()).atZone(VIETNAM).toOffsetDateTime();
            if (classes.resourceConflict(s.trainerId(), s.roomId(), start, end)) {
                throw new ConflictException("Trùng phòng hoặc huấn luyện viên khi sinh buổi ngày " + date + ".");
            }
            inserted += classes.insertSession(s, date, start, end);
        }
        return inserted;
    }
}
