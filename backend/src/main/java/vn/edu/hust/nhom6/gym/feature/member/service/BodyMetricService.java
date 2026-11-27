package vn.edu.hust.nhom6.gym.feature.member.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.dto.BodyMetricDto;
import vn.edu.hust.nhom6.gym.feature.member.dto.BodyMetricRequest;
import vn.edu.hust.nhom6.gym.feature.member.entity.BodyMetric;
import vn.edu.hust.nhom6.gym.feature.member.repository.BodyMetricRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.service.TrainerStudentAccess;

/**
 * Chỉ số cơ thể của hội viên (F2 UC-F2.5; F9 UC-F9.5). Lớp riêng, không sửa {@link MemberService} (điểm nóng).
 *
 * <p>Quyền (S3):
 * <ul>
 *   <li>PT: chỉ học viên mình dạy, qua {@link TrainerStudentAccess} (BR-F9.10): không dạy cùng chi nhánh 403, khác
 *       chi nhánh 404.</li>
 *   <li>Lễ tân, quản lý: hội viên chi nhánh mình; chủ chuỗi mọi chi nhánh; ngoài phạm vi 404
 *       ({@link MemberService#requireInStaffScope}, {@link MemberService#requireReadable}).</li>
 *   <li>Hội viên: chỉ xem của mình, người khác 404; không tự ghi (controller chặn 403).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BodyMetricService {
    private final MemberService members;
    private final TrainerStudentAccess trainerAccess;
    private final BodyMetricRepository metrics;
    private final VnTime vnTime;

    /** Ghi một lần đo; {@code recorded_by} = người đang gọi API. */
    @Transactional
    public BodyMetricDto record(CurrentUser actor, Long memberId, BodyMetricRequest request) {
        if (actor.is(UserRole.TRAINER)) {
            trainerAccess.requireTrainerTeaching(actor, memberId);
        } else {
            members.requireInStaffScope(actor, memberId);
        }
        if (!request.hasAnyMetric()) {
            throw new InvalidRequestException("Vui lòng nhập ít nhất một chỉ số cơ thể.");
        }
        if (request.measuredAt().isAfter(vnTime.today())) {
            throw new InvalidRequestException("Ngày đo không được sau hôm nay.");
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().strip();
        BodyMetric saved = metrics.saveAndFlush(BodyMetric.record(memberId, actor.userId(), request.measuredAt(),
            request.weight(), request.height(), request.bodyFat(), request.muscleMass(), note));
        return toDto(saved);
    }

    /** Lịch sử đo, mới nhất trước (UC-F2.5 bước 3). */
    public List<BodyMetricDto> history(CurrentUser actor, Long memberId) {
        if (actor.is(UserRole.TRAINER)) {
            trainerAccess.requireTrainerTeaching(actor, memberId);
        } else {
            members.requireReadable(actor, memberId);
        }
        return metrics.findByMemberIdOrderByMeasuredAtDescIdDesc(memberId).stream()
            .map(BodyMetricService::toDto).toList();
    }

    /**
     * Các lần đo từ ngày {@code from} đến hết ngày {@code to} (truy vấn nửa mở {@code < to + 1}), cũ trước, cho biểu
     * đồ tiến độ F9. <b>Không kiểm tra quyền</b>: nơi gọi ({@code ProgressService}) đã kiểm tra.
     */
    public List<BodyMetricDto> findInRange(Long memberId, LocalDate from, LocalDate to) {
        return metrics.findByMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtAscIdAsc(
                memberId, from, to.plusDays(1)).stream()
            .map(BodyMetricService::toDto).toList();
    }

    private static BodyMetricDto toDto(BodyMetric m) {
        return new BodyMetricDto(m.getId(), m.getMemberId(), m.getMeasuredAt(), m.getWeight(), m.getHeight(),
            m.getBodyFat(), m.getMuscleMass(), m.getNote(), m.getRecordedBy(), m.getCreatedAt());
    }
}
