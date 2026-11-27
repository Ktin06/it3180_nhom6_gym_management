package vn.edu.hust.nhom6.gym.feature.trainer.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;
import vn.edu.hust.nhom6.gym.common.exception.ForbiddenException;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.exception.NotFoundException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService.StaffIdentity;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.ClassSessionScheduleItem;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.PtSessionScheduleItem;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerAvailabilityDto;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerAvailabilityRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerScheduleResponse;
import vn.edu.hust.nhom6.gym.feature.trainer.entity.TrainerAvailability;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerAuditRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerAvailabilityRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerScheduleLookupRepository;
import vn.edu.hust.nhom6.gym.feature.trainer.repository.TrainerScheduleLookupRepository.BookedPtSlot;

/**
 * Quản lý lịch rảnh và lịch dạy của huấn luyện viên (F9, UC-F9.1) và đọc khung rảnh cho hội viên đặt buổi (UC-F9.2).
 *
 * <p>Quy tắc chính:
 * <ul>
 *   <li>Mã PT ({@code staff.id}) luôn lấy từ JWT qua {@link StaffService}, không nhận từ body (BR-F9.1).</li>
 *   <li>Giờ kết thúc phải sau giờ bắt đầu (400, E1).</li>
 *   <li>Hai khung của cùng PT cùng thứ không chồng nhau (422, BR-F9.14, E4); hai PT khác nhau trùng giờ vẫn hợp lệ.</li>
 *   <li>Xóa (xóa hẳn dòng + ghi {@code audit_logs}) và sửa thu hẹp chỉ cho phép khi không làm buổi PT {@code BOOKED}
 *       chưa diễn ra rơi ra ngoài khung theo giờ Việt Nam (422, BR-F9.15, E5).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrainerAvailabilityService {

    static final String AVAILABILITY_NOT_FOUND = "Khung giờ rảnh không tồn tại.";
    static final String TRAINER_NOT_FOUND = "Huấn luyện viên không tồn tại.";
    static final String OVERLAP_MESSAGE = "Khung giờ bị trùng với khung rảnh đã khai báo.";
    static final String BOOKED_SESSION_BLOCKED = "Khung giờ đã có hội viên đặt, không thể xóa hoặc thu hẹp.";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int DEFAULT_SCHEDULE_DAYS = 6;

    private final TrainerAvailabilityRepository availabilities;
    private final TrainerScheduleLookupRepository lookup;
    private final TrainerAuditRepository audit;
    private final StaffService staffService;
    private final VnTime vnTime;
    private final JsonMapper json;

    /** PT khai báo khung giờ rảnh mới (201 Created). */
    @Transactional
    public TrainerAvailabilityDto create(CurrentUser me, TrainerAvailabilityRequest request) {
        StaffIdentity staff = staffService.requireActiveStaff(me.userId());
        Long branchId = resolveBranchId(staff, request.branchId());
        validateTimeRange(request.startTime(), request.endTime());

        lookup.lockTrainer(staff.staffId());
        rejectOverlap(staff.staffId(), request.dayOfWeek(), request.startTime(), request.endTime(), null);

        TrainerAvailability saved = availabilities.saveAndFlush(TrainerAvailability.create(
            staff.staffId(), branchId, request.dayOfWeek(), request.startTime(), request.endTime()));
        return toDto(saved);
    }

    /** PT sửa khung giờ rảnh của chính mình (200 OK). */
    @Transactional
    public TrainerAvailabilityDto update(CurrentUser me, Long id, TrainerAvailabilityRequest request) {
        StaffIdentity staff = staffService.requireActiveStaff(me.userId());
        TrainerAvailability availability = availabilities.findById(id)
            .orElseThrow(() -> new NotFoundException(AVAILABILITY_NOT_FOUND));
        if (!availability.getTrainerId().equals(staff.staffId())) {
            throw new ForbiddenException("Bạn không có quyền sửa khung giờ rảnh của huấn luyện viên khác.");
        }
        Long branchId = resolveBranchId(staff, request.branchId());
        validateTimeRange(request.startTime(), request.endTime());

        lookup.lockTrainer(staff.staffId());
        rejectOverlap(staff.staffId(), request.dayOfWeek(), request.startTime(), request.endTime(), availability.getId());

        TrainerAvailability candidate = TrainerAvailability.create(
            staff.staffId(), branchId, request.dayOfWeek(), request.startTime(), request.endTime());
        List<BookedPtSlot> futureBooked = lookup.findFutureBookedPtSessions(staff.staffId(), vnTime.now());
        for (BookedPtSlot session : futureBooked) {
            if (availability.overlapsInVnTime(session.startAt(), session.endAt())
                    && !candidate.coversInVnTime(session.startAt(), session.endAt())) {
                throw new BusinessRuleException(BOOKED_SESSION_BLOCKED);
            }
        }

        availability.update(branchId, request.dayOfWeek(), request.startTime(), request.endTime());
        return toDto(availabilities.saveAndFlush(availability));
    }

    /** PT xóa hẳn khung giờ rảnh khi không còn buổi PT BOOKED tương lai nằm trong khung, kèm ghi audit_logs (BR-F9.15). */
    @Transactional
    public void delete(CurrentUser me, Long id) {
        StaffIdentity staff = staffService.requireActiveStaff(me.userId());
        TrainerAvailability availability = availabilities.findById(id)
            .orElseThrow(() -> new NotFoundException(AVAILABILITY_NOT_FOUND));
        if (!availability.getTrainerId().equals(staff.staffId())) {
            throw new ForbiddenException("Bạn không có quyền xóa khung giờ rảnh của huấn luyện viên khác.");
        }

        lookup.lockTrainer(staff.staffId());
        OffsetDateTime now = vnTime.now();
        List<BookedPtSlot> futureBooked = lookup.findFutureBookedPtSessions(staff.staffId(), now);
        boolean hasAffectedSession = futureBooked.stream()
            .anyMatch(session -> availability.overlapsInVnTime(session.startAt(), session.endAt()));
        if (hasAffectedSession) {
            throw new BusinessRuleException(BOOKED_SESSION_BLOCKED);
        }

        String oldValueJson = serializeAvailability(availability);
        Long availabilityId = availability.getId();
        Long branchId = availability.getBranchId();
        availabilities.delete(availability);
        availabilities.flush();
        audit.insertAvailabilityDeleted(me.userId(), branchId, availabilityId, oldValueJson, now);
    }

    /**
     * Lịch dạy của PT trong khoảng ngày Việt Nam {@code [from, to]} gồm khung rảnh, buổi PT và buổi lớp nhóm được
     * phân công (UC-F9.1). Thiếu {@code from}/{@code to} mặc định 7 ngày từ hôm nay giờ Việt Nam.
     */
    public TrainerScheduleResponse getSchedule(CurrentUser me, LocalDate from, LocalDate to) {
        StaffIdentity staff = staffService.requireActiveStaff(me.userId());
        LocalDate resolvedFrom = from != null ? from : (to != null ? to.minusDays(DEFAULT_SCHEDULE_DAYS) : vnTime.today());
        LocalDate resolvedTo = to != null ? to : resolvedFrom.plusDays(DEFAULT_SCHEDULE_DAYS);
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new InvalidRequestException("Ngày bắt đầu không được sau ngày kết thúc.");
        }

        OffsetDateTime rangeStart = vnTime.dayStart(resolvedFrom);
        OffsetDateTime rangeEnd = vnTime.dayEnd(resolvedTo);

        List<TrainerAvailabilityDto> slots = availabilities
            .findByTrainerIdOrderByDayOfWeekAscStartTimeAscIdAsc(staff.staffId())
            .stream().map(this::toDto).toList();
        List<PtSessionScheduleItem> ptSessions = lookup.findPtSessionsInRange(staff.staffId(), rangeStart, rangeEnd)
            .stream()
            .map(r -> new PtSessionScheduleItem(r.id(), r.memberId(), r.memberCode(), r.memberName(),
                r.membershipId(), r.branchId(), r.startAt(), r.endAt(), r.status(), r.note()))
            .toList();
        List<ClassSessionScheduleItem> classSessions = lookup
            .findClassSessionsInRange(staff.staffId(), rangeStart, rangeEnd)
            .stream()
            .map(r -> new ClassSessionScheduleItem(r.id(), r.classScheduleId(), r.branchId(), r.classTypeId(),
                r.classTypeName(), r.roomId(), r.roomName(), r.occurrenceDate(), r.startAt(), r.endAt(),
                r.originalStartAt(), r.capacity(), r.status()))
            .toList();

        return new TrainerScheduleResponse(staff.staffId(), staff.branchId(), resolvedFrom, resolvedTo,
            slots, ptSessions, classSessions);
    }

    /** Danh sách khung giờ rảnh của một huấn luyện viên đang hoạt động (cho hội viên xem trước khi đặt buổi PT). */
    public List<TrainerAvailabilityDto> listForTrainer(Long trainerId) {
        var trainer = lookup.findTrainer(trainerId)
            .filter(TrainerScheduleLookupRepository.TrainerSummary::isActiveTrainer)
            .orElseThrow(() -> new NotFoundException(TRAINER_NOT_FOUND));
        return availabilities.findByTrainerIdOrderByDayOfWeekAscStartTimeAscIdAsc(trainer.id())
            .stream().map(this::toDto).toList();
    }

    /**
     * Kiểm tra khoảng thời gian {@code [startAt, endAt]} có nằm gọn trong một khung rảnh của huấn luyện viên hay không
     * (tính theo thứ và giờ Việt Nam). Dùng chung cho thẻ đặt buổi PT ({@code S2-19.2}, BR-F9.2).
     */
    public boolean isWithinAvailability(Long trainerId, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (trainerId == null || startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            return false;
        }
        int vnDayOfWeek = startAt.atZoneSameInstant(VnTime.ZONE).getDayOfWeek().getValue();
        return availabilities.findByTrainerIdAndDayOfWeekOrderByStartTimeAscIdAsc(trainerId, vnDayOfWeek)
            .stream()
            .anyMatch(slot -> slot.coversInVnTime(startAt, endAt));
    }

    private Long resolveBranchId(StaffIdentity staff, Long requestedBranchId) {
        if (requestedBranchId == null) {
            return staff.branchId();
        }
        if (!requestedBranchId.equals(staff.branchId())) {
            throw new ForbiddenException("Huấn luyện viên chỉ được khai báo khung giờ rảnh tại chi nhánh làm việc của mình.");
        }
        return requestedBranchId;
    }

    private static void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidRequestException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
    }

    private void rejectOverlap(Long trainerId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime,
            Long excludeId) {
        List<TrainerAvailability> sameDay = availabilities
            .findByTrainerIdAndDayOfWeekOrderByStartTimeAscIdAsc(trainerId, dayOfWeek);
        boolean overlapped = sameDay.stream()
            .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
            .anyMatch(existing -> existing.overlaps(dayOfWeek, startTime, endTime));
        if (overlapped) {
            throw new BusinessRuleException(OVERLAP_MESSAGE);
        }
    }

    private String serializeAvailability(TrainerAvailability availability) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", availability.getId());
        snapshot.put("trainerId", availability.getTrainerId());
        snapshot.put("branchId", availability.getBranchId());
        snapshot.put("dayOfWeek", availability.getDayOfWeek());
        snapshot.put("startTime", availability.getStartTime().format(TIME_FORMAT));
        snapshot.put("endTime", availability.getEndTime().format(TIME_FORMAT));
        return json.writeValueAsString(snapshot);
    }

    private TrainerAvailabilityDto toDto(TrainerAvailability availability) {
        return new TrainerAvailabilityDto(
            availability.getId(),
            availability.getTrainerId(),
            availability.getBranchId(),
            availability.getDayOfWeek(),
            availability.getStartTime(),
            availability.getEndTime(),
            availability.getCreatedAt(),
            availability.getUpdatedAt());
    }
}
