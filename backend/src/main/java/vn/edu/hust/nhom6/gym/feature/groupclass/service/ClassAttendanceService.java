package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.AttendanceRequest;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.AttendanceRoster;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.AttendanceRow;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.AttendanceStatus;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.Session;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

@Service
@Transactional
@RequiredArgsConstructor
public class ClassAttendanceService {
    private final ClassRepository classes;
    private final ClassBookingService bookings;
    private final StaffService staff;
    private final VnTime time;

    @Transactional(readOnly = true)
    public AttendanceRoster roster(CurrentUser actor, long sessionId) {
        Session session = classes.session(sessionId, false);
        requireRosterAccess(actor, session);
        return new AttendanceRoster(session, classes.attendance(sessionId));
    }

    public AttendanceRow mark(CurrentUser actor, long sessionId, AttendanceRequest request) {
        requireRosterAccess(actor, classes.session(sessionId, false));
        var observed = classes.attendanceRow(sessionId, request.bookingId());

        classes.lockMember(observed.memberId());
        Session session = classes.session(sessionId, true);
        requireRosterAccess(actor, session);
        var row = classes.attendanceRow(sessionId, request.bookingId());
        var now = time.now();
        boolean afterDay = !now.isBefore(time.dayEnd(time.toVnDate(session.startAt())));
        boolean trainer = actor.is(UserRole.TRAINER);
        boolean first = "BOOKED".equals(row.status());
        boolean cancelled = "CANCELLED".equals(session.status()) || "CANCELLED".equals(row.status());
        boolean trainerOutsideAttendanceWindow = trainer && (now.isBefore(session.startAt()) || afterDay);
        boolean managerOutsideCorrectionWindow = !trainer && (!afterDay || first);
        if (cancelled || trainerOutsideAttendanceWindow || managerOutsideCorrectionWindow) {
            throw new BusinessRuleException("Không thể điểm danh với thời gian hoặc trạng thái hiện tại.");
        }

        if (!row.updatedAt().isEqual(request.expectedUpdatedAt())) {
            throw staleAttendance();
        }
        if (row.status().equals(request.status().name())) {
            return row;
        }
        if (!classes.markAttendance(row, request.status(), actor.userId())) {
            throw staleAttendance();
        }
        if (!first) {
            classes.auditAttendance(row, request.status(), session.branchId(), actor.userId(), now);
        } else if (request.status() == AttendanceStatus.NO_SHOW) {
            bookings.applyPenalty(row.memberId(), session.branchId(), actor.userId(), now);
        }
        return classes.attendanceRow(sessionId, request.bookingId());
    }

    public void unlock(CurrentUser actor, long memberId) {
        if (!actor.is(UserRole.BRANCH_MANAGER)) {
            throw new ForbiddenException("Chỉ quản lý chi nhánh được gỡ khóa đặt lớp.");
        }
        long branch = staff.requireBranchId(actor.userId());
        classes.lockMember(memberId);
        var member = classes.member(memberId);
        if (member.homeBranchId() != branch) {
            throw new ForbiddenException("Hội viên không thuộc chi nhánh của người quản lý.");
        }
        var now = time.now();
        if (member.bookingLockedUntil() != null && member.bookingLockedUntil().isAfter(now)) {
            classes.unlockBooking(memberId, branch, actor.userId(), member.bookingLockedUntil(), now);
        }
    }

    private void requireRosterAccess(CurrentUser actor, Session session) {
        long branch = session.branchId();
        if ((actor.is(UserRole.TRAINER) && staff.requireBranchId(actor.userId()) == branch
                && classes.teaches(session.id(), actor.userId()))
                || (actor.is(UserRole.BRANCH_MANAGER) && staff.requireBranchId(actor.userId()) == branch)) {
            return;
        }
        throw new ForbiddenException("Bạn không được phân công hoặc không quản lý chi nhánh của buổi học.");
    }

    private static ConflictException staleAttendance() {
        return new ConflictException("Chỗ đặt vừa được cập nhật. Vui lòng tải lại danh sách điểm danh.");
    }
}
