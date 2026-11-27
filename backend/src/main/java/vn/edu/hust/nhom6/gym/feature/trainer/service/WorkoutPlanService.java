package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutPlanDto;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutPlanRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutPlanRequest.PlanSession;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.WorkoutPlan;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerStudentLookupRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.WorkoutPlanRepository;

/**
 * Giáo án của PT cho học viên (UC-F9.4). Quyền: chỉ học viên PT dạy ({@link TrainerStudentAccess}, BR-F9.10).
 * Một hội viên có nhiều giáo án theo thời gian, chỉ thêm mới (GĐ-3 của S2-20.1).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutPlanService {
    private static final TypeReference<List<PlanSession>> CONTENT_TYPE = new TypeReference<>() { };

    private final TrainerStudentAccess access;
    private final WorkoutPlanRepository plans;
    private final TrainerStudentLookupRepository lookup;
    private final JsonMapper json;

    @Transactional
    public WorkoutPlanDto create(CurrentUser trainer, Long memberId, WorkoutPlanRequest request) {
        TrainerStudentAccess.TrainerIdentity me = access.requireTrainerTeaching(trainer, memberId);
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new InvalidRequestException("Ngày kết thúc không được trước ngày bắt đầu.");
        }
        WorkoutPlan plan = plans.saveAndFlush(WorkoutPlan.create(me.staffId(), memberId, request.title().trim(),
            json.writeValueAsString(request.content()), request.startDate(), request.endDate()));
        return toDto(plan, lookup.findStaffName(me.staffId()).orElse(null));
    }

    /** Mọi giáo án của học viên (kể cả do PT khác soạn), mới nhất trước. */
    public List<WorkoutPlanDto> list(CurrentUser trainer, Long memberId) {
        access.requireTrainerTeaching(trainer, memberId);
        Map<Long, String> names = new HashMap<>();
        return plans.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
            .map(p -> toDto(p, names.computeIfAbsent(p.getTrainerId(), id -> lookup.findStaffName(id).orElse(null))))
            .toList();
    }

    private WorkoutPlanDto toDto(WorkoutPlan plan, String trainerName) {
        List<PlanSession> content = plan.getContent() == null ? List.of()
            : json.readValue(plan.getContent(), CONTENT_TYPE);
        return new WorkoutPlanDto(plan.getId(), plan.getTrainerId(), trainerName, plan.getMemberId(), plan.getTitle(),
            content, plan.getStartDate(), plan.getEndDate(), plan.getCreatedAt());
    }
}
