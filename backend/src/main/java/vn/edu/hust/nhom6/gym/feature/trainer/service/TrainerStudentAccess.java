package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerStudentDto;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerStudentLookupRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerStudentLookupRepository.MemberRow;

/**
 * Kiểm tra "huấn luyện viên có dạy hội viên này không" (BR-F9.10, S3, VD-26 vấn đề 1). Dùng chung cho giáo án, nhật ký
 * tập, chỉ số cơ thể (F2 gọi qua service này) và tiến độ. "Dạy" chỉ tính buổi PT chưa hủy ({@code BOOKED},
 * {@code DONE}, {@code NO_SHOW}) giữa huấn luyện viên và hội viên, bỏ nhánh lớp nhóm (chi tiết ở
 * {@link TrainerStudentLookupRepository}).
 *
 * <p>Thứ tự lỗi (GĐ-2 của S2-20.1, VD-26 vấn đề 2 giữ nguyên):
 * <ol>
 *   <li>Hội viên không có hoặc đã xóa mềm → 404.</li>
 *   <li>PT có dạy (có buổi PT chưa hủy) → cho qua, kể cả hội viên chi nhánh khác (gói PT toàn chuỗi).</li>
 *   <li>Không dạy, hội viên thuộc chi nhánh gốc khác chi nhánh của PT → 404 (giấu hồ sơ ngoài chi nhánh như F2).</li>
 *   <li>Không dạy, cùng chi nhánh → 403.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainerStudentAccess {
    public static final String NOT_TEACHING = "Bạn chỉ thao tác được với học viên mình dạy.";
    public static final String MEMBER_NOT_FOUND = "Không tìm thấy hội viên.";

    private final TrainerStudentLookupRepository lookup;
    private final StaffService staff;

    /**
     * Bảo đảm huấn luyện viên {@code trainerStaffId} ({@code staff.id}) dạy hội viên {@code memberId}.
     *
     * @throws NotFoundException (404) hội viên không có, đã xóa mềm, hoặc khác chi nhánh mà PT không dạy
     * @throws ForbiddenException (403) cùng chi nhánh nhưng PT không dạy
     */
    public void requireTeaches(Long trainerStaffId, Long memberId) {
        MemberRow member = lookup.findMember(memberId).orElseThrow(TrainerStudentAccess::notFound);
        if (lookup.teaches(trainerStaffId, memberId)) {
            return;
        }
        Long trainerBranch = lookup.findStaffBranchId(trainerStaffId).orElse(null);
        if (!Objects.equals(trainerBranch, member.homeBranchId())) {
            throw notFound();
        }
        throw new ForbiddenException(NOT_TEACHING);
    }

    /**
     * Như {@link #requireTeaches} nhưng nhận người gọi API vai trò {@code TRAINER}: đổi {@code users.id} trong token
     * thành {@code staff.id}. Trả {@code staff.id} và chi nhánh làm việc để nơi gọi ghi dữ liệu.
     *
     * @throws org.springframework.security.access.AccessDeniedException (403) không có dòng staff ACTIVE
     */
    public StaffService.StaffIdentity requireTrainerTeaching(CurrentUser trainer, Long memberId) {
        StaffService.StaffIdentity me = staff.requireActiveStaff(trainer.userId());
        requireTeaches(me.staffId(), memberId);
        return me;
    }

    /** Học viên của huấn luyện viên đang gọi API (GĐ-10), sắp theo họ tên. */
    public List<TrainerStudentDto> myStudents(CurrentUser trainer) {
        return lookup.findStudents(staff.requireStaffId(trainer.userId()));
    }

    private static NotFoundException notFound() {
        return new NotFoundException(MEMBER_NOT_FOUND);
    }
}
