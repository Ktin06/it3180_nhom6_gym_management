package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutLogDto;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutLogRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSession;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.PtSessionStatus;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.WorkoutLog;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.PtSessionRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerAuditRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.WorkoutLogRepository;

/**
 * Nhật ký tập do PT ghi (UC-F9.5, BR-F9.10, BR-F9.11). Mỗi request một bài tập, một dòng. Ghi kèm {@code audit_logs}
 * cùng giao dịch, vì {@code workout_logs} không có cột người ghi (GĐ-11 của S2-20.1).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutLogService {
    static final String SESSION_NOT_OWNED = "Buổi PT không thuộc huấn luyện viên và hội viên này.";
    static final String SESSION_NOT_DONE = "Chỉ ghi nhật ký cho buổi PT đã xác nhận diễn ra.";
    static final String SESSION_OTHER_DAY = "Ngày tập phải trùng ngày diễn ra buổi PT.";

    private final TrainerStudentAccess access;
    private final WorkoutLogRepository logs;
    private final PtSessionRepository ptSessions;
    private final TrainerAuditRepository audit;
    private final VnTime vnTime;
    private final JsonMapper json;

    @Transactional
    public WorkoutLogDto create(CurrentUser trainer, Long memberId, WorkoutLogRequest request) {
        StaffService.StaffIdentity me = access.requireTrainerTeaching(trainer, memberId);
        if (request.loggedAt().isAfter(vnTime.today())) {
            throw new InvalidRequestException("Ngày tập không được sau hôm nay.");
        }
        if (request.ptSessionId() != null) {
            requireLoggableSession(request.ptSessionId(), me.staffId(), memberId, request.loggedAt());
        }
        WorkoutLog log = logs.saveAndFlush(WorkoutLog.create(memberId, request.ptSessionId(), request.loggedAt(),
            request.exercise().trim(), request.sets(), request.reps(), request.weight()));

        Map<String, Object> newValue = new LinkedHashMap<>();
        newValue.put("memberId", memberId);
        newValue.put("ptSessionId", log.getPtSessionId());
        newValue.put("loggedAt", log.getLoggedAt().toString());
        newValue.put("exercise", log.getExercise());
        audit.insertWorkoutLogCreated(trainer.userId(), me.branchId(), log.getId(), json.writeValueAsString(newValue),
            vnTime.now());
        return toDto(log);
    }

    /**
     * Nhật ký của hội viên từ ngày {@code from} đến hết ngày {@code to} (truy vấn nửa mở {@code < to + 1}), ngày tăng
     * dần. Nơi gọi đã kiểm tra quyền.
     */
    public List<WorkoutLogDto> findInRange(Long memberId, LocalDate from, LocalDate to) {
        return logs.findByMemberIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAscIdAsc(memberId,
                from, to.plusDays(1)).stream()
            .map(WorkoutLogService::toDto).toList();
    }

    /**
     * Buổi PT gắn với nhật ký phải của đúng PT và hội viên (SRS UC-F9.5 mục F), đã {@code DONE} (bàn giao S2-19.3), và
     * {@code loggedAt} là ngày Việt Nam của buổi (GĐ-4). Không có buổi cũng trả 422, không 404, để không nhầm với
     * "hội viên không tồn tại".
     */
    private void requireLoggableSession(Long sessionId, Long trainerId, Long memberId, LocalDate loggedAt) {
        PtSession session = ptSessions.findById(sessionId)
            .filter(s -> s.getTrainerId().equals(trainerId) && s.getMemberId().equals(memberId))
            .orElseThrow(() -> new BusinessRuleException(SESSION_NOT_OWNED));
        if (session.getStatus() != PtSessionStatus.DONE) {
            throw new BusinessRuleException(SESSION_NOT_DONE);
        }
        if (!vnTime.toVnDate(session.getStartAt()).equals(loggedAt)) {
            throw new BusinessRuleException(SESSION_OTHER_DAY);
        }
    }

    private static WorkoutLogDto toDto(WorkoutLog log) {
        return new WorkoutLogDto(log.getId(), log.getMemberId(), log.getPtSessionId(), log.getLoggedAt(),
            log.getExercise(), log.getSets(), log.getReps(), log.getWeight(), log.getCreatedAt());
    }
}
