package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService.PtDeduction;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionCompletionDto;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSession;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSessionStatus;
import vn.edu.hust.nhom6.gym.feature.trainer.event.PtSessionCancelledByPackageEndEvent;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.PtCommissionLookupRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.PtSessionRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerAuditRepository;

/**
 * Xác nhận buổi PT đã diễn ra ({@code DONE}) hoặc ghi vắng ({@code NO_SHOW}) (UC-F9.3, BR-F9.6–F9.9, BR-F9.18,
 * BR-F9.19, VD-21 GĐ-8). Vai trò kiểm ở controller ({@code TRAINER}, {@code BRANCH_MANAGER}); service kiểm phân công,
 * chi nhánh, giờ, hạn.
 *
 * <p><b>Thứ tự (NF6, khớp check-in F4: members → memberships → pt_sessions):</b>
 * <ol>
 *   <li>Đọc buổi không khóa: 404, 403, không còn {@code BOOKED} → 422, chưa tới giờ → 422, PT quá hạn → 422.</li>
 *   <li>Khóa {@code members}. Đọc lại buổi: lúc này mới thấy không còn {@code BOOKED} → 409 (thao tác khác vừa commit
 *       trong lúc chờ khóa; E4).</li>
 *   <li>Trừ buổi qua {@link MembershipService#deductPtSession} (khóa {@code memberships}; G13).</li>
 *   <li>Khóa dòng {@code pt_sessions}, kiểm lại {@code BOOKED} (luồng hủy chỉ khóa dòng này) → 409, rollback cả việc trừ.</li>
 *   <li>Ghi {@code DONE} + hoa hồng hoặc {@code NO_SHOW}. Gói vừa hết buổi → xử lý buổi tương lai (GĐ-8). Quản lý → audit.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PtSessionCompletionService {
    static final String NOT_STARTED = "Buổi tập chưa diễn ra.";
    static final String PAST_DEADLINE = "Đã quá hạn xác nhận, liên hệ quản lý chi nhánh";
    static final String ALREADY_COMPLETED = "Buổi PT đã được xác nhận hoặc ghi vắng, không trừ buổi lần hai.";
    static final String ALREADY_CANCELLED = "Buổi PT đã bị hủy.";
    static final String CHANGED_CONCURRENTLY =
        "Buổi PT vừa được một thao tác khác cập nhật. Vui lòng tải lại danh sách buổi.";

    /** Kết quả buổi PT mà PT (hoặc quản lý) ghi nhận. */
    public enum Outcome { DONE, NO_SHOW }

    private final PtSessionRepository ptSessions;
    private final PtCommissionLookupRepository commissions;
    private final TrainerAuditRepository audit;
    private final MemberService members;
    private final MembershipService memberships;
    private final StaffService staffService;
    private final ApplicationEventPublisher events;
    private final EntityManager entities;
    private final VnTime vnTime;
    private final JsonMapper json;

    /**
     * Xác nhận ({@link Outcome#DONE}) hoặc ghi vắng ({@link Outcome#NO_SHOW}) một buổi PT. Cả hai trừ đúng một buổi;
     * chỉ {@code DONE} có hoa hồng.
     *
     * @throws ForbiddenException (403, E2) không phải PT được phân công, hoặc quản lý chi nhánh khác
     * @throws NotFoundException (404, E3) buổi không tồn tại
     * @throws ConflictException (409, E4) thao tác khác vừa đổi trạng thái buổi
     * @throws BusinessRuleException (422, E5) chưa tới giờ, đã xác nhận/ghi vắng, đã hủy, PT quá hạn, gói không trừ được
     */
    @Transactional
    public PtSessionCompletionDto complete(CurrentUser me, Long sessionId, Outcome outcome) {
        PtSession session = ptSessions.findById(sessionId)
            .orElseThrow(() -> new NotFoundException(PtSessionService.SESSION_NOT_FOUND));
        boolean byManager = me.is(UserRole.BRANCH_MANAGER);
        requireScope(me, session, byManager);
        requireBookedOr422(session);
        OffsetDateTime now = vnTime.now();
        if (now.isBefore(session.getStartAt())) {
            throw new BusinessRuleException(NOT_STARTED);
        }
        LocalDate sessionDate = vnTime.toVnDate(session.getStartAt());
        if (!byManager && !now.isBefore(vnTime.dayEnd(sessionDate))) {
            throw new BusinessRuleException(PAST_DEADLINE);
        }

        members.lockForUpdate(session.getMemberId());
        entities.refresh(session);
        requireStillBooked(session);

        PtDeduction deduction = memberships.deductPtSession(session.getMembershipId(), sessionDate, vnTime.today());

        entities.refresh(session, LockModeType.PESSIMISTIC_WRITE);
        requireStillBooked(session);
        if (outcome == Outcome.DONE) {
            int commission = PtCommission.calculate(
                commissions.find(session.getTrainerId(), session.getMembershipId()));
            session.confirm(now, commission);
        } else {
            session.markNoShow(now);
        }
        ptSessions.saveAndFlush(session);

        List<Long> moved = new ArrayList<>();
        List<Long> cancelled = new ArrayList<>();
        if (deduction.expiredNow()) {
            relocateUpcoming(deduction, now, moved, cancelled);
        }
        if (byManager) {
            audit.insertPtSessionCompleted(me.userId(), session.getBranchId(), session.getId(),
                outcome == Outcome.DONE ? TrainerAuditRepository.ACTION_CONFIRM : TrainerAuditRepository.ACTION_NO_SHOW,
                json.writeValueAsString(Map.of("status", PtSessionStatus.BOOKED.name())), completedValue(session),
                now);
        }
        Membership deducted = deduction.deducted();
        Membership next = deduction.activatedNext();
        return PtSessionCompletionDto.of(session, deducted.getRemainingSessions(), deducted.getStatus(),
            next == null ? null : next.getId(), moved, cancelled);
    }

    /**
     * VD-21 GĐ-8 = A (trưởng nhóm chốt 20/11, giữ làm lưới an toàn sau VD-23 lựa chọn D): buổi {@code BOOKED} chưa diễn
     * ra của gói vừa hết buổi. Sớm trước: chuyển sang gói nối tiếp vừa ACTIVE nếu gói đó còn chỗ (BR-F9.4: tổng số buổi
     * {@code BOOKED} của gói < số buổi còn lại) và ngày của buổi còn trong hạn gói; không thì hủy (không trừ buổi) và
     * phát sự kiện cho F10.
     */
    private void relocateUpcoming(PtDeduction deduction, OffsetDateTime now, List<Long> moved, List<Long> cancelled) {
        List<PtSession> upcoming = ptSessions.findUpcomingBookedForUpdate(deduction.deducted().getId(), now);
        if (upcoming.isEmpty()) {
            return;
        }
        Membership next = deduction.activatedNext();
        long free = next == null ? 0
            : next.getRemainingSessions() - ptSessions.countBooked(next.getId());
        for (PtSession s : upcoming) {
            if (free > 0 && !vnTime.toVnDate(s.getStartAt()).isAfter(next.getEndDate())) {
                s.moveToMembership(next.getId());
                free--;
                moved.add(s.getId());
            } else {
                s.cancel(now);
                cancelled.add(s.getId());
                events.publishEvent(new PtSessionCancelledByPackageEndEvent(s.getId(), s.getMemberId(),
                    s.getTrainerId(), s.getBranchId(), s.getStartAt(), deduction.deducted().getId(), now));
            }
        }
        ptSessions.saveAllAndFlush(upcoming);
    }

    private void requireScope(CurrentUser me, PtSession session, boolean byManager) {
        if (byManager) {
            if (!staffService.requireBranchId(me.userId()).equals(session.getBranchId())) {
                throw new ForbiddenException("Buổi PT thuộc chi nhánh khác.");
            }
        } else if (!staffService.requireStaffId(me.userId()).equals(session.getTrainerId())) {
            throw new ForbiddenException("Bạn chỉ được xác nhận buổi PT mình được phân công.");
        }
    }

    private static void requireBookedOr422(PtSession session) {
        switch (session.getStatus()) {
            case BOOKED -> { }
            case CANCELLED -> throw new BusinessRuleException(ALREADY_CANCELLED);
            default -> throw new BusinessRuleException(ALREADY_COMPLETED);
        }
    }

    private static void requireStillBooked(PtSession session) {
        if (session.getStatus() != PtSessionStatus.BOOKED) {
            throw new ConflictException(CHANGED_CONCURRENTLY);
        }
    }

    private String completedValue(PtSession session) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", session.getStatus().name());
        value.put("confirmedAt", session.getConfirmedAt().toString());
        value.put("commissionAmount", session.getCommissionAmount());
        return json.writeValueAsString(value);
    }
}
