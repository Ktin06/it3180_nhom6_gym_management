package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.*;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.nhom6.gym.common.exception.*;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;
import static vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassRepository;

/**
 * Quản lý chỗ đặt lớp học nhóm (F8) và phục vụ check-in tại quầy (F4).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClassBookingService {
    private final ClassRepository classes;
    private final StaffService staff;
    private final VnTime time;
    private final SystemConfigService configs;
    private static final Set<String> OPEN = Set.of("SCHEDULED", "RESCHEDULED");

    /**
     * Ghi {@code arrived_at = now} cho các chỗ BOOKED của hội viên ở buổi thuộc chi nhánh {@code branchId}, bắt đầu trong
     * {@code [from, to]} (S2-11.1, UC-F4.3). Khung giờ do F4 truyền vào; F8 chỉ ghi cột này, không đổi {@code status}.
     * Chỗ đã NO_SHOW, ATTENDED, CANCELLED, hoặc đã có {@code arrived_at} không bị đụng (kể cả {@code updated_at}).
     *
     * <p>Nơi gọi giữ khóa {@code members} (và {@code memberships} nếu có) trước, đúng thứ tự members → memberships →
     * class_bookings, và {@code UPDATE} khóa các dòng {@code class_bookings} khớp điều kiện. Gọi trong giao dịch của nơi gọi.
     *
     * @return id các chỗ vừa ghi, tăng dần; rỗng nếu không có
     */
    public List<Long> markArrived(Long memberId, Long branchId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime now) {
        return classes.markArrived(memberId, branchId, from, to, now);
    }

    private ClassRepository.MemberRow authorize(CurrentUser actor, long memberId, Session session) {
        var member = classes.member(memberId);
        if (actor.role() == UserRole.MEMBER && actor.userId().equals(member.userId())) { return member; }
        if (actor.role() == UserRole.RECEPTIONIST && staff.requireBranchId(actor.userId()).equals(session.branchId())
                && member.homeBranchId().equals(session.branchId())) { return member; }
        throw new ForbiddenException("Bạn không có quyền thao tác chỗ đặt của hội viên này tại chi nhánh.");
    }

    /** NF6: khóa hội viên trước buổi học, rồi kiểm tra lại lịch và sĩ số trong giao dịch. */
    private Session lock(long memberId, long sessionId) {
        classes.lockMember(memberId);
        return classes.session(sessionId, true);
    }

    public Booking book(CurrentUser actor, long sessionId, long memberId) {
        authorize(actor, memberId, classes.session(sessionId, false));
        var session = lock(memberId, sessionId);
        var member = authorize(actor, memberId, session);
        var now = time.now();
        var today = time.toVnDate(now);
        var day = time.toVnDate(session.startAt());
        long branch = session.branchId();
        if (!OPEN.contains(session.status()) || day.isBefore(today)
                || day.isAfter(today.plusDays(config(branch, BOOKING_WINDOW_DAYS)))
                || now.isAfter(session.startAt().minusMinutes(config(branch, BOOKING_CUTOFF_MIN)))) {
            throw new BusinessRuleException("Buổi học đã đóng đặt chỗ hoặc ngoài cửa sổ đặt chỗ.");
        }
        if (classes.duplicate(memberId, sessionId)) {
            throw new ConflictException("Hội viên đã có chỗ đặt của buổi này.");
        }
        if (!"ACTIVE".equals(member.status())) {
            throw new BusinessRuleException("Hội viên không ở trạng thái hoạt động.");
        }
        if (member.bookingLockedUntil() != null && member.bookingLockedUntil().isAfter(now)) {
            throw new BusinessRuleException("Hội viên đang bị khóa đặt lớp.");
        }
        if (!classes.hasActiveTimePackage(memberId, branch, day)) {
            throw new BusinessRuleException("Hội viên cần gói TIME đang ACTIVE, dùng được tại chi nhánh và còn hiệu lực ngày học.");
        }
        if (classes.overlap(memberId, session)) {
            throw new ConflictException("Hội viên đã có lịch trùng giờ. Hãy hủy buổi kia trước.");
        }
        if (classes.occupied(sessionId) >= session.capacity()) {
            throw new ConflictException("Lớp đã đủ chỗ");
        }
        return classes.booking(classes.insertBooking(sessionId, memberId, actor.userId(), now));
    }

    @Transactional(readOnly = true)
    public Booking detail(CurrentUser actor, long id) {
        var b = classes.booking(id);
        authorize(actor, b.memberId(), classes.session(b.classSessionId(), false));
        return b;
    }

    public Booking cancel(CurrentUser actor, long id) {
        var observed = classes.booking(id);
        authorize(actor, observed.memberId(), classes.session(observed.classSessionId(), false));
        var session = lock(observed.memberId(), observed.classSessionId());
        var b = classes.booking(id);
        authorize(actor, b.memberId(), session);
        var now = time.now();
        if (!"BOOKED".equals(b.status()) || !OPEN.contains(session.status()) || !session.startAt().isAfter(now)) {
            throw new BusinessRuleException("Chỗ đặt đã kết thúc hoặc không thể hủy.");
        }
        boolean late = session.originalStartAt() == null
            && now.isAfter(session.startAt().minusHours(config(session.branchId(), LATE_CANCEL_HOURS)));
        classes.cancel(id, late, actor.role() == UserRole.RECEPTIONIST, now);
        if (late) { applyPenalty(b.memberId(), session.branchId(), actor.userId(), now); }
        return classes.booking(id);
    }

    private int config(long branch, String key) {
        int value = configs.getInt(branch, key);
        boolean positiveRequired = key.equals(PENALTY_LIMIT_PER_MONTH) || key.equals(PENALTY_LOCK_DAYS);
        if (value < (positiveRequired ? 1 : 0) || value > 3660) {
            throw new IllegalStateException("Cấu hình không hợp lệ: " + key);
        }
        return value;
    }

    private void applyPenalty(long memberId, long branch, long actor, OffsetDateTime now) {
        var member = classes.member(memberId);
        if (member.bookingLockedUntil() != null && member.bookingLockedUntil().isAfter(now)) { return; }
        var month = time.dayStart(time.toVnDate(now).withDayOfMonth(1));
        var last = classes.latestLock(memberId);
        // Không gửi OffsetDateTime.MIN xuống JDBC; mốc đầu tháng đủ khi chưa có lịch sử khóa.
        if (last.isBefore(month.minusNanos(1000))) { last = month.minusNanos(1000); }
        if (classes.violations(memberId, month, month.plusMonths(1), last)
                >= config(branch, PENALTY_LIMIT_PER_MONTH)) {
            classes.lockBooking(memberId, branch, actor, now, now.plusDays(config(branch, PENALTY_LOCK_DAYS)));
        }
    }
}
