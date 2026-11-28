package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.auth.entity.UserRole;
import vn.edu.hust.nhom6.gym.feature.member.entity.Member;
import vn.edu.hust.nhom6.gym.feature.member.service.BodyMetricService;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberService;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.ProgressDto;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerStudentLookupRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerStudentLookupRepository.MemberRow;

/**
 * Tiến độ học viên (UC-F9.6). Trả dữ liệu thô theo ngày: chỉ số cơ thể (F2, qua {@link BodyMetricService}) và nhật ký
 * tập; không gom, không gợi ý (BR-F9.13, GĐ-8 của S2-20.1).
 *
 * <p>Quyền: PT chỉ xem học viên mình dạy ({@link TrainerStudentAccess}, BR-F9.10); hội viên chỉ xem của mình, id người
 * khác trả 404 ({@link MemberService#requireReadable}, BR-F9.12). Vai trò khác bị {@code @PreAuthorize} chặn (GĐ-9).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {
    /** Bỏ trống khoảng ngày thì lấy 90 ngày đến hôm nay. */
    static final int DEFAULT_DAYS = 90;
    /** Khoảng dài nhất, tính cả hai đầu. */
    static final int MAX_DAYS = 366;

    private final TrainerStudentAccess access;
    private final MemberService members;
    private final BodyMetricService bodyMetrics;
    private final WorkoutLogService workoutLogs;
    private final TrainerStudentLookupRepository lookup;
    private final VnTime vnTime;

    public ProgressDto forMember(CurrentUser me, Long memberId, LocalDate from, LocalDate to) {
        if (me.is(UserRole.TRAINER)) {
            access.requireTrainerTeaching(me, memberId);
        } else if (me.is(UserRole.MEMBER)) {
            members.requireReadable(me, memberId);
        } else {
            throw new ForbiddenException("Bạn không có quyền xem tiến độ hội viên.");
        }
        return build(memberId, from, to);
    }

    /** Hội viên đang đăng nhập xem của chính mình; hội viên lấy từ token, không nhận id. */
    public ProgressDto mine(CurrentUser me, LocalDate from, LocalDate to) {
        Member member = members.requireByUserId(me.userId());
        return build(member.getId(), from, to);
    }

    private ProgressDto build(Long memberId, LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : vnTime.today();
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new InvalidRequestException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_DAYS) {
            throw new InvalidRequestException("Khoảng thời gian xem tiến độ tối đa " + MAX_DAYS + " ngày.");
        }
        MemberRow member = lookup.findMember(memberId)
            .orElseThrow(() -> new NotFoundException(TrainerStudentAccess.MEMBER_NOT_FOUND));
        return new ProgressDto(member.id(), member.memberCode(), member.fullName(), start, end,
            bodyMetrics.findInRange(memberId, start, end), workoutLogs.findInRange(memberId, start, end));
    }
}
