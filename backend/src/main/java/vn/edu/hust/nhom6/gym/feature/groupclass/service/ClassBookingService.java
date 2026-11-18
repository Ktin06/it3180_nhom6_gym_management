package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberActor;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffLookupService;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassBookingService {
    private final ClassRepository classes;
    private final StaffLookupService staff;
    private final Clock classClock;
    private static final Set<String> OPEN = Set.of("SCHEDULED", "RESCHEDULED");

    private void authorize(MemberActor actor, long memberId, Session s) {
        var member = classes.member(memberId);
        if (actor.role() == UserRole.MEMBER && actor.userId().equals(member.userId())) { return; }
        if (actor.role() == UserRole.RECEPTIONIST && staff.activeBranchIdForUser(actor.userId()).equals(s.branchId())
                && member.homeBranchId().equals(s.branchId())) { return; }
        throw new ForbiddenException("Bạn không có quyền thao tác chỗ đặt của hội viên này tại chi nhánh.");
    }

    /** Snapshot ứng viên, khóa member theo ID rồi khóa buổi; thay đổi tập ứng viên trả 409 để thử lại. */
    private Session lock(long memberId, long sessionId) {
        Set<Long> locked = new TreeSet<>(classes.waitlistMembers(sessionId));
        locked.add(memberId);
        classes.lockMembers(locked);
        var session = classes.session(sessionId, true);
        if (!locked.containsAll(classes.waitlistMembers(sessionId))) {
            throw new ConflictException("Danh sách chờ vừa thay đổi. Vui lòng thử lại.");
        }
        return session;
    }

    public Booking book(MemberActor actor, long sessionId, long memberId) {
        authorize(actor, memberId, classes.session(sessionId, false));
        var session = lock(memberId, sessionId);
        authorize(actor, memberId, session);
        var now = OffsetDateTime.now(classClock);
        var today = now.atZoneSameInstant(ClassScheduleService.VIETNAM).toLocalDate();
        var day = session.startAt().atZoneSameInstant(ClassScheduleService.VIETNAM).toLocalDate();
        if (!OPEN.contains(session.status()) || day.isBefore(today)
                || day.isAfter(today.plusDays(classes.config(session.branchId(), "BOOKING_WINDOW_DAYS")))
                || !beforeCutoff(session, now)) {
            throw new BusinessRuleException("Buổi học đã đóng đặt chỗ hoặc ngoài cửa sổ đặt chỗ.");
        }
        if (classes.duplicate(memberId, sessionId)) { throw new ConflictException("Hội viên đã có chỗ đặt hoặc đang trong danh sách chờ của buổi này."); }
        if (!classes.eligible(memberId, session.branchId(), day, now)) {
            throw new BusinessRuleException("Hội viên cần gói TIME đang ACTIVE tại chi nhánh, còn hiệu lực ngày học và không bị khóa đặt lớp.");
        }
        if (classes.overlap(memberId, session)) { throw new ConflictException("Hội viên đã có lịch trùng giờ. Hãy hủy buổi kia trước."); }
        promoteWaiting(session, now);
        long id = classes.insertBooking(sessionId, memberId, actor.userId(),
            classes.occupied(sessionId) >= session.capacity(), now);
        return classes.booking(id);
    }

    @Transactional(readOnly = true)
    public Booking detail(MemberActor actor, long id) {
        var b = classes.booking(id);
        authorize(actor, b.memberId(), classes.session(b.classSessionId(), false));
        return b;
    }

    public Booking cancel(MemberActor actor, long id) {
        var observed = classes.booking(id);
        authorize(actor, observed.memberId(), classes.session(observed.classSessionId(), false));
        var session = lock(observed.memberId(), observed.classSessionId());
        var b = classes.booking(id);
        authorize(actor, b.memberId(), session);
        var now = OffsetDateTime.now(classClock);
        if (!Set.of("BOOKED", "WAITLIST").contains(b.status()) || !OPEN.contains(session.status())
                || !session.startAt().isAfter(now)) {
            throw new BusinessRuleException("Chỗ đặt đã kết thúc hoặc không thể hủy.");
        }
        boolean late = b.status().equals("BOOKED")
            && now.isAfter(session.startAt().minusHours(classes.config(session.branchId(), "LATE_CANCEL_HOURS")))
            && (b.promotedAt() == null || b.graceUntil() == null || now.isAfter(b.graceUntil()));
        classes.cancel(id, late, actor.role() == UserRole.RECEPTIONIST, now);
        if (late) { applyPenalty(b.memberId(), session.branchId(), actor.userId(), now); }
        if (b.status().equals("BOOKED")) { promoteWaiting(session, now); }
        return classes.booking(id);
    }

    private boolean beforeCutoff(Session s, OffsetDateTime now) {
        return !now.isAfter(s.startAt().minusMinutes(classes.config(s.branchId(), "PROMOTION_CUTOFF_MIN")));
    }

    /** Dùng chung cho đặt mới và hủy; caller đã khóa toàn bộ ứng viên trước khóa buổi. */
    private void promoteWaiting(Session s, OffsetDateTime now) {
        if (!OPEN.contains(s.status()) || !beforeCutoff(s, now)) { return; }
        long free = s.capacity() - classes.occupied(s.id());
        if (free <= 0) { return; }
        var day = s.startAt().atZoneSameInstant(ClassScheduleService.VIETNAM).toLocalDate();
        for (var b : classes.waitlist(s.id())) {
            if (free <= 0) { break; }
            if (!classes.eligible(b.memberId(), s.branchId(), day, now) || classes.overlap(b.memberId(), s)) { continue; }
            classes.promote(b, now, classes.config(s.branchId(), "PROMOTION_GRACE_MIN"));
            classes.promotionNotice(b, s, now);
            free--;
        }
    }

    private void applyPenalty(long memberId, long branch, long actor, OffsetDateTime now) {
        var member = classes.member(memberId);
        if (member.bookingLockedUntil() != null && member.bookingLockedUntil().isAfter(now)) { return; }
        var month = now.atZoneSameInstant(ClassScheduleService.VIETNAM).toLocalDate().withDayOfMonth(1)
            .atStartOfDay(ClassScheduleService.VIETNAM).toOffsetDateTime();
        var last = classes.latestLock(memberId);
        // Không gửi OffsetDateTime.MIN xuống JDBC; mốc đầu tháng đủ khi chưa có lịch sử khóa.
        if (last.isBefore(month.minusNanos(1000))) { last = month.minusNanos(1000); }
        if (classes.violations(memberId, month, month.plusMonths(1), last)
                >= classes.config(branch, "PENALTY_LIMIT_PER_MONTH")) {
            classes.lockBooking(memberId, branch, actor, now, now.plusDays(classes.config(branch, "PENALTY_LOCK_DAYS")));
        }
    }
}
