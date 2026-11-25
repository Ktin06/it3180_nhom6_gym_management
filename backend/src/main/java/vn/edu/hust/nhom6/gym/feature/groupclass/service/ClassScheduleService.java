package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ConflictException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassScheduleDto;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassScheduleRequest;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassSchedule;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassScheduleStatus;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ScheduleSlot;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassScheduleRepository;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.GroupClassLookupRepository;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.GroupClassLookupRepository.RoomRow;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;

/**
 * Quản lý lịch lặp lớp nhóm (UC-F8.7, phần lịch lặp; S2-16.1). Vai trò kiểm ở controller: ghi chỉ
 * {@code BRANCH_MANAGER}, xem thêm {@code SUPER_ADMIN}. Service kiểm phạm vi chi nhánh (BR-F8-04): lịch và tài nguyên
 * ngoài chi nhánh của quản lý trả 404 như thể không tồn tại.
 *
 * <p>Thứ tự kiểm tra khi tạo, sửa: tài nguyên tồn tại trong chi nhánh (404) → trạng thái, vai trò, giờ, hiệu lực, sức
 * chứa (422) → khóa phòng và người dạy → xung đột (409). Lỗi nào cũng không ghi gì.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassScheduleService {
    static final String SCHEDULE_NOT_FOUND = "Lịch lớp không tồn tại.";

    private final ClassScheduleRepository schedules;
    private final GroupClassLookupRepository lookup;
    private final ClassConflictChecker conflicts;
    private final StaffService staffService;
    private final ClassSessionGenerationService generation;

    /**
     * Huấn luyện viên {@code trainerId} ({@code staff.id}) có dạy lớp nhóm chồng giờ {@code [start, end)} không (F9 đặt
     * buổi PT, BR-F9.3, NF6). Xét buổi lớp chưa hủy và lịch lặp ACTIVE chưa sinh buổi; buổi chạm biên không tính.
     * Chỉ đọc, không khóa. Không kiểm vai trò: hàm dùng giữa hai service.
     *
     * @return câu mô tả buổi hoặc lịch bị trùng; rỗng nếu không trùng
     */
    public Optional<String> findTrainerTeachingConflict(Long trainerId, OffsetDateTime start,
            OffsetDateTime end) {
        return conflicts.forTrainerTeaching(trainerId, start, end);
    }

    /**
     * Tạo lịch ACTIVE trong chi nhánh của quản lý (AC-F8.7.1-01), rồi sinh ngay buổi 14 ngày tới trong cùng giao dịch
     * (M2-L1); tác vụ nền 00:05 sinh tiếp các ngày sau.
     */
    @Transactional
    public ClassScheduleDto create(CurrentUser me, ClassScheduleRequest request) {
        Long branchId = staffService.requireBranchId(me.userId());
        ScheduleSlot slot = request.toSlot();
        validate(branchId, slot);
        lookup.lockRoomAndTrainer(slot.roomId(), slot.trainerId());
        rejectConflict(slot, null);
        ClassSchedule created = schedules.saveAndFlush(ClassSchedule.create(branchId, slot));
        generation.generateUpcomingFor(created.getId());
        return toDto(created);
    }

    /**
     * Thay dữ liệu lịch ACTIVE. Buổi đã sinh giữ nguyên (AC-F8.7.1-05); ngày chưa có buổi trong 14 ngày tới được sinh
     * ngay theo dữ liệu mới (M2-L1).
     */
    @Transactional
    public ClassScheduleDto update(CurrentUser me, Long id, ClassScheduleRequest request) {
        Long branchId = staffService.requireBranchId(me.userId());
        ClassSchedule schedule = lockInBranch(id, branchId);
        if (!schedule.isActive()) {
            throw new BusinessRuleException("Lịch lớp đã ngừng, không sửa được.");
        }
        ScheduleSlot slot = request.toSlot();
        validate(branchId, slot);
        lookup.lockRoomAndTrainer(slot.roomId(), slot.trainerId());
        rejectConflict(slot, schedule.getId());
        schedule.change(slot);
        ClassSchedule saved = schedules.saveAndFlush(schedule);
        generation.generateUpcomingFor(saved.getId());
        return toDto(saved);
    }

    /** Ngừng lịch: INACTIVE, không sinh thêm buổi; buổi và chỗ đặt đã có giữ nguyên (BR-F8.7-04). */
    @Transactional
    public ClassScheduleDto deactivate(CurrentUser me, Long id) {
        Long branchId = staffService.requireBranchId(me.userId());
        ClassSchedule schedule = lockInBranch(id, branchId);
        if (!schedule.isActive()) {
            throw new BusinessRuleException("Lịch lớp đã ngừng.");
        }
        schedule.deactivate();
        return toDto(schedules.saveAndFlush(schedule));
    }

    /** Một lịch: quản lý chỉ thấy lịch chi nhánh mình, chủ chuỗi thấy mọi chi nhánh. */
    public ClassScheduleDto get(CurrentUser me, Long id) {
        Long branchId = scopeOf(me);
        return schedules.findById(id)
            .filter(s -> branchId == null || s.getBranchId().equals(branchId))
            .map(this::toDto)
            .orElseThrow(() -> new NotFoundException(SCHEDULE_NOT_FOUND));
    }

    /**
     * Danh sách lịch, theo thứ rồi giờ bắt đầu. Quản lý: chi nhánh mình ({@code branchId} truyền vào bị bỏ qua).
     * Chủ chuỗi: mọi chi nhánh, lọc theo {@code branchId} nếu có. {@code status} rỗng = mọi trạng thái.
     */
    public List<ClassScheduleDto> list(CurrentUser me, Long branchId, ClassScheduleStatus status) {
        Long scope = me.isOwner() ? branchId : staffService.requireBranchId(me.userId());
        return toDtos(schedules.search(scope, status));
    }

    private Long scopeOf(CurrentUser me) {
        return me.isOwner() ? null : staffService.requireBranchId(me.userId());
    }

    private ClassSchedule lockInBranch(Long id, Long branchId) {
        return schedules.findByIdForUpdate(id)
            .filter(s -> s.getBranchId().equals(branchId))
            .orElseThrow(() -> new NotFoundException(SCHEDULE_NOT_FOUND));
    }

    /** Kiểm tra tài nguyên và quy tắc BR-F8.7-02, BR-F8.7-03 (AC-F8.7.1-02, phạm vi tài nguyên của AC-F8.7.1-08). */
    private void validate(Long branchId, ScheduleSlot slot) {
        var classType = lookup.findClassType(slot.classTypeId())
            .orElseThrow(() -> new NotFoundException("Loại lớp không tồn tại."));
        RoomRow room = lookup.findRoom(slot.roomId())
            .filter(r -> r.branchId().equals(branchId))
            .orElseThrow(() -> new NotFoundException("Phòng không tồn tại trong chi nhánh."));
        var trainer = lookup.findTrainer(slot.trainerId())
            .filter(t -> t.branchId().equals(branchId))
            .orElseThrow(() -> new NotFoundException("Huấn luyện viên không tồn tại trong chi nhánh."));

        if (!classType.active()) {
            throw new BusinessRuleException("Loại lớp đã ngừng hoạt động.");
        }
        if (!room.active()) {
            throw new BusinessRuleException("Phòng đã ngừng sử dụng.");
        }
        if (!trainer.active()) {
            throw new BusinessRuleException("Huấn luyện viên đã nghỉ việc.");
        }
        if (!trainer.isTrainer()) {
            throw new BusinessRuleException("Người dạy phải là huấn luyện viên.");
        }
        if (!slot.endTime().isAfter(slot.startTime())) {
            throw new BusinessRuleException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (slot.validTo() != null && slot.validTo().isBefore(slot.validFrom())) {
            throw new BusinessRuleException("Ngày kết thúc hiệu lực không được trước ngày bắt đầu.");
        }
        if (slot.capacity() > room.capacity()) {
            throw new BusinessRuleException("Sức chứa lớp vượt sức chứa phòng (%d chỗ).".formatted(room.capacity()));
        }
    }

    private void rejectConflict(ScheduleSlot slot, Long scheduleId) {
        conflicts.forSchedule(slot, scheduleId).ifPresent(message -> {
            throw new ConflictException(message);
        });
    }

    private ClassScheduleDto toDto(ClassSchedule schedule) {
        return toDtos(List.of(schedule)).getFirst();
    }

    private List<ClassScheduleDto> toDtos(List<ClassSchedule> list) {
        Map<Long, String> types = lookup.classTypeNames(ids(list, ClassSchedule::getClassTypeId));
        Map<Long, String> trainers = lookup.trainerNames(ids(list, ClassSchedule::getTrainerId));
        Map<Long, String> rooms = lookup.roomNames(ids(list, ClassSchedule::getRoomId));
        return list.stream().map(s -> new ClassScheduleDto(s.getId(), s.getBranchId(),
                s.getClassTypeId(), types.get(s.getClassTypeId()),
                s.getTrainerId(), trainers.get(s.getTrainerId()),
                s.getRoomId(), rooms.get(s.getRoomId()),
                s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), s.getCapacity(),
                s.getValidFrom(), s.getValidTo(), s.getStatus(), s.getCreatedAt(), s.getUpdatedAt()))
            .toList();
    }

    private static Set<Long> ids(List<ClassSchedule> list, Function<ClassSchedule, Long> field) {
        return list.stream().map(field).collect(Collectors.toSet());
    }
}
