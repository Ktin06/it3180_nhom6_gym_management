package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.ClassScheduleService;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.entity.MemberStatus;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.membership.entity.Membership;
import vn.edu.hust.nhom6.gym.feature.membership.service.MembershipService;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionBookRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionDto;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSession;
import vn.edu.hust.nhom6.gym.feature.trainer.event.PtSessionCancelledByManagerEvent;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.PtSessionRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerAuditRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerScheduleLookupRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerScheduleLookupRepository.TrainerSummary;

/**
 * Đặt buổi PT (UC-F9.2) và hủy buổi PT (UC-F9.7). Vai trò kiểm ở controller; service kiểm quyền sở hữu và chi nhánh.
 *
 * <p><b>Thứ tự khi đặt (NF6, ERD ghi chú {@code pt_sessions}):</b>
 * <ol>
 *   <li>Kiểm tra không cần khóa: hồ sơ hội viên, huấn luyện viên (404), đặt trước đủ {@code PT_MIN_LEAD_HOURS}.</li>
 *   <li>Đọc trùng buổi PT <b>trước khi khóa</b>: thấy ngay thì 422 (E5, trùng lịch có sẵn).</li>
 *   <li>Khóa {@code members} ({@code FOR UPDATE}) rồi {@code staff} ({@code FOR NO KEY UPDATE}, cùng kiểu khóa với
 *       lịch rảnh S2-19.1 và đổi giờ buổi lớp S2-17.1).</li>
 *   <li>Đọc lại trùng buổi PT: lúc này mới thấy nghĩa là một yêu cầu khác vừa commit trong lúc chờ khóa → 409 (E4).</li>
 *   <li>Giờ rảnh, giờ dạy lớp nhóm (chỉ đọc {@code class_sessions}, không khóa: đổi giờ lớp khóa
 *       {@code class_sessions → rooms → staff}, khóa thêm {@code class_sessions} ở đây sẽ khóa chéo), chỗ lớp của
 *       hội viên, gói PT và số buổi đã đặt (BR-F9.4) → 422.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PtSessionService {
    static final String SESSION_NOT_FOUND = "Buổi tập không tồn tại.";

    private final PtSessionRepository ptSessions;
    private final TrainerScheduleLookupRepository lookup;
    private final TrainerAuditRepository audit;
    private final TrainerAvailabilityService availabilities;
    private final ClassScheduleService classSchedules;
    private final MemberService members;
    private final MembershipService memberships;
    private final StaffService staffService;
    private final SystemConfigService configs;
    private final ApplicationEventPublisher events;
    private final VnTime vnTime;
    private final JsonMapper json;

    /**
     * Hội viên đặt buổi PT 60 phút (Q9 = A). Chỉ giữ chỗ, không trừ buổi (BR-F9.5).
     *
     * @throws ForbiddenException (403) tài khoản chưa gắn hồ sơ hội viên
     * @throws NotFoundException (404, E3) huấn luyện viên không có, đã nghỉ, hoặc không phải vai trò TRAINER
     * @throws ConflictException (409, E4) yêu cầu khác vừa đặt cùng khung giờ của PT hoặc của hội viên
     * @throws BusinessRuleException (422, E5) hồ sơ ngừng, đặt sát giờ, trùng lịch, ngoài giờ rảnh, không có gói PT,
     *         đã đặt đủ số buổi còn lại
     */
    @Transactional
    public PtSessionDto book(CurrentUser me, PtSessionBookRequest request) {
        Member member = members.requireByUserId(me.userId());
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessRuleException("Hồ sơ hội viên đang ngừng hoạt động, không đặt được buổi PT.");
        }
        TrainerSummary trainer = lookup.findTrainer(request.trainerId())
            .filter(TrainerSummary::isActiveTrainer)
            .orElseThrow(() -> new NotFoundException(TrainerAvailabilityService.TRAINER_NOT_FOUND));
        OffsetDateTime start = request.startAt().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime end = PtSession.endOf(start);
        OffsetDateTime now = vnTime.now();
        int leadHours = configs.getInt(trainer.branchId(), SystemConfigKeys.PT_MIN_LEAD_HOURS);
        if (start.isBefore(now.plusHours(leadHours))) {
            throw new BusinessRuleException(
                "Buổi PT phải được đặt trước giờ bắt đầu ít nhất %d giờ.".formatted(leadHours));
        }

        List<PtSession> overlapBeforeLock = ptSessions.findBookedOverlap(trainer.id(), member.getId(), start, end);
        if (!overlapBeforeLock.isEmpty()) {
            throw new BusinessRuleException(describeOverlap(overlapBeforeLock.getFirst(), member.getId()));
        }

        members.lockForUpdate(member.getId());
        lookup.lockTrainer(trainer.id());

        List<PtSession> overlapAfterLock = ptSessions.findBookedOverlap(trainer.id(), member.getId(), start, end);
        if (!overlapAfterLock.isEmpty()) {
            throw new ConflictException("Khung giờ này vừa được đặt. Vui lòng chọn giờ khác.");
        }
        if (!availabilities.isWithinAvailability(trainer.id(), start, end)) {
            throw new BusinessRuleException("Giờ đặt nằm ngoài giờ rảnh của huấn luyện viên.");
        }
        classSchedules.findTrainerTeachingConflict(trainer.id(), start, end).ifPresent(conflict -> {
            throw new BusinessRuleException("Huấn luyện viên có lịch dạy lớp nhóm trùng giờ. " + conflict);
        });
        if (lookup.hasMemberClassBookingOverlap(member.getId(), start, end)) {
            throw new BusinessRuleException("Bạn đã có lịch lớp nhóm trùng giờ này.");
        }
        Membership membership = pickMembership(member.getId(), trainer.branchId(), start);

        PtSession saved = ptSessions.saveAndFlush(PtSession.book(trainer.id(), member.getId(), membership.getId(),
            trainer.branchId(), start, me.userId()));
        return PtSessionDto.of(saved);
    }

    /**
     * Hủy buổi PT đang {@code BOOKED} (UC-F9.7). Không trừ buổi, không phạt.
     * <ul>
     *   <li>Hội viên (buổi của mình), lễ tân (chi nhánh của buổi): còn ít nhất {@code PT_MIN_LEAD_HOURS} giờ (BR-F9.16).</li>
     *   <li>Quản lý chi nhánh của buổi: trước giờ bắt đầu, bắt buộc lý do, ghi {@code audit_logs}, phát
     *       {@link PtSessionCancelledByManagerEvent} cho F10 (BR-F9.17).</li>
     * </ul>
     *
     * @throws InvalidRequestException (400, E6) quản lý không nhập lý do
     * @throws ForbiddenException (403, E1) buổi của hội viên khác, chi nhánh khác, hoặc vai trò khác
     * @throws NotFoundException (404, E2) buổi không tồn tại
     * @throws BusinessRuleException (422) buổi không còn BOOKED (E3), hủy sát giờ (E4), đã tới giờ (E5)
     */
    @Transactional
    public PtSessionDto cancel(CurrentUser me, Long sessionId, String reason) {
        boolean byManager = me.role() == UserRole.BRANCH_MANAGER;
        String trimmedReason = reason == null ? null : reason.strip();
        PtSession session = ptSessions.findByIdForUpdate(sessionId)
            .orElseThrow(() -> new NotFoundException(SESSION_NOT_FOUND));
        requireCanCancel(me, session);
        // Kiểm lý do sau quyền: buổi không có hoặc của chi nhánh khác thì trả 404/403 trước 400.
        if (byManager && (trimmedReason == null || trimmedReason.isEmpty())) {
            throw new InvalidRequestException("Vui lòng nhập lý do hủy buổi PT.");
        }
        session.requireBooked();

        OffsetDateTime now = vnTime.now();
        if (byManager) {
            if (!now.isBefore(session.getStartAt())) {
                throw new BusinessRuleException("Buổi PT đã bắt đầu, không hủy được.");
            }
        } else {
            int leadHours = configs.getInt(session.getBranchId(), SystemConfigKeys.PT_MIN_LEAD_HOURS);
            if (session.getStartAt().isBefore(now.plusHours(leadHours))) {
                String message = "Chỉ được hủy buổi PT trước giờ bắt đầu ít nhất %d giờ.".formatted(leadHours);
                throw new BusinessRuleException(me.role() == UserRole.MEMBER
                    ? message + " Vui lòng liên hệ quầy." : message);
            }
        }

        session.cancel(now);
        ptSessions.saveAndFlush(session);
        if (byManager) {
            audit.insertPtSessionCancelled(me.userId(), session.getBranchId(), session.getId(),
                json.writeValueAsString(Map.of("status", "BOOKED")),
                cancelledValue(session, trimmedReason), now);
            events.publishEvent(new PtSessionCancelledByManagerEvent(session.getId(), session.getMemberId(),
                trimmedReason, session.getTrainerId(), session.getBranchId(), session.getStartAt(), me.userId(), now));
        }
        return PtSessionDto.of(session);
    }

    /**
     * Gói PT dùng được tại chi nhánh buổi vào ngày Việt Nam của buổi, còn chỗ cho thêm một buổi (BR-F9.4, VD-23 lựa
     * chọn D): tổng số buổi {@code BOOKED} của gói (mọi giờ, kể cả buổi đã qua giờ mà PT chưa xác nhận) phải ít hơn
     * {@code remaining_sessions}. Nhiều gói thì lấy gói hết hạn sớm nhất còn chỗ.
     */
    private Membership pickMembership(Long memberId, Long branchId, OffsetDateTime start) {
        List<Membership> usable = memberships.findUsable(memberId, branchId, PackageType.PT, vnTime.toVnDate(start));
        if (usable.isEmpty()) {
            throw new BusinessRuleException("Bạn không có gói PT còn hiệu lực dùng được tại chi nhánh của huấn luyện "
                + "viên vào ngày tập.");
        }
        return usable.stream()
            .filter(m -> m.getRemainingSessions() != null
                && ptSessions.countBooked(m.getId()) < m.getRemainingSessions())
            .findFirst()
            .orElseThrow(() -> new BusinessRuleException(
                "Bạn đã đặt đủ số buổi còn lại của gói PT. Vui lòng gia hạn gói để đặt thêm."));
    }

    private void requireCanCancel(CurrentUser me, PtSession session) {
        switch (me.role()) {
            case MEMBER -> {
                Member member = members.requireByUserId(me.userId());
                if (!member.getId().equals(session.getMemberId())) {
                    throw new ForbiddenException("Bạn chỉ được hủy buổi PT của mình.");
                }
            }
            case RECEPTIONIST, BRANCH_MANAGER -> {
                if (!staffService.requireBranchId(me.userId()).equals(session.getBranchId())) {
                    throw new ForbiddenException("Buổi PT thuộc chi nhánh khác.");
                }
            }
            default -> throw new ForbiddenException("Bạn không có quyền hủy buổi PT.");
        }
    }

    private static String describeOverlap(PtSession other, Long memberId) {
        return other.getMemberId().equals(memberId)
            ? "Bạn đã có buổi PT trùng giờ này."
            : "Huấn luyện viên đã có buổi PT trùng giờ này.";
    }

    private String cancelledValue(PtSession session, String reason) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", session.getStatus().name());
        value.put("reason", reason);
        value.put("cancelledAt", session.getCancelledAt().toString());
        return json.writeValueAsString(value);
    }
}
