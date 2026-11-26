package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.BranchOption;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.CalendarSession;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.MemberCalendar;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassCalendarRepository;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassCalendarService {
    private static final int DEFAULT_DAYS = 7;
    private static final int MAX_DAYS = 31;
    private final ClassCalendarRepository calendars;
    private final VnTime time;
    private final StaffService staff;

    public List<BranchOption> branches() {
        return calendars.branches();
    }

    public List<CalendarSession> publicSessions(Long branchId, LocalDate from, LocalDate to) {
        Range range = range(from, to);
        requireActiveBranch(branchId);
        return calendars.publicSessions(branchId, range.start(), range.end());
    }

    public MemberCalendar memberSessions(CurrentUser actor, Long branchId, LocalDate from, LocalDate to) {
        if (!actor.is(UserRole.MEMBER)) {
            throw new ForbiddenException("Chỉ hội viên được xem lịch đặt lớp của mình.");
        }
        Range range = range(from, to);
        requireActiveBranch(branchId);
        long memberId = calendars.memberId(actor.userId());
        return new MemberCalendar(memberId, calendars.memberSessions(memberId, branchId, range.start(), range.end()));
    }

    public List<CalendarSession> trainerSessions(CurrentUser actor, LocalDate from, LocalDate to) {
        if (!actor.is(UserRole.TRAINER)) {
            throw new ForbiddenException("Chỉ huấn luyện viên được xem lịch dạy của mình.");
        }
        Range range = range(from, to);
        return calendars.trainerSessions(actor.userId(), staff.requireBranchId(actor.userId()), range.start(), range.end());
    }

    private void requireActiveBranch(Long branchId) {
        if (branchId != null && !calendars.activeBranch(branchId)) {
            throw new NotFoundException("Không tìm thấy chi nhánh đang hoạt động.");
        }
    }

    private Range range(LocalDate from, LocalDate to) {
        var start = from == null ? time.today() : from;
        var end = to == null ? start.plusDays(DEFAULT_DAYS - 1) : to;
        if (start.getYear() < 1 || start.getYear() > 9999 || end.getYear() > 9999
                || end.isBefore(start) || !end.isBefore(start.plusDays(MAX_DAYS))) {
            throw new InvalidRequestException("Khoảng lịch phải có tối đa 31 ngày và ngày kết thúc không trước ngày bắt đầu.");
        }
        return new Range(time.dayStart(start), time.dayEnd(end));
    }

    private record Range(OffsetDateTime start, OffsetDateTime end) {}
}
