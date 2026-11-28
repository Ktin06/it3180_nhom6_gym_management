package vn.edu.hust.nhom6.gym.feature.groupclass.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.*;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "F8 – Lịch lớp và đặt chỗ")
@RequiredArgsConstructor
public class ClassController {
    private final ClassScheduleService schedules;
    private final ClassCalendarService calendars;
    private final ClassBookingService bookings;
    private final ClassSessionService sessions;
    private final ClassAttendanceService attendance;

    @GetMapping("/public/class-branches")
    @Operation(summary = "Danh sách chi nhánh đang hoạt động để lọc lịch lớp")
    public List<BranchOption> publicBranches() {
        return calendars.branches();
    }

    @GetMapping("/public/class-sessions")
    @Operation(summary = "Lịch lớp công khai theo chi nhánh và khoảng ngày Việt Nam")
    public List<CalendarSession> publicCalendar(@RequestParam(required = false) @Positive Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendars.publicSessions(branchId, from, to);
    }

    @GetMapping("/members/me/class-sessions")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Lịch lớp, chỗ trống và trạng thái chỗ đặt của hội viên đang đăng nhập")
    public MemberCalendar memberCalendar(Authentication auth, @RequestParam(required = false) @Positive Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendars.memberSessions(CurrentUser.from(auth), branchId, from, to);
    }

    @GetMapping("/trainers/me/class-sessions")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "Lịch dạy lớp nhóm theo phân công từng buổi của huấn luyện viên")
    public List<CalendarSession> trainerCalendar(Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendars.trainerSessions(CurrentUser.from(auth), from, to);
    }

    @GetMapping("/class-schedules")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Danh sách lịch lặp tại chi nhánh quản lý; page từ 0, size tối đa 100")
    public PageResponse<Schedule> schedules(Authentication auth,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return schedules.list(CurrentUser.from(auth), page, size);
    }

    @PostMapping("/class-schedules")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Tạo lịch lặp; thứ ISO 1–7, giờ Việt Nam; chi nhánh lấy từ nhân viên")
    public ResponseEntity<Schedule> create(Authentication auth, @Valid @RequestBody ScheduleRequest request) {
        var s = schedules.save(CurrentUser.from(auth), null, request);
        return ResponseEntity.created(URI.create("/api/v1/class-schedules/" + s.id())).body(s);
    }

    @GetMapping("/class-schedules/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Xem lịch lặp trong chi nhánh quản lý")
    public Schedule scheduleDetail(Authentication auth, @PathVariable @Positive Long id) {
        return schedules.detail(CurrentUser.from(auth), id);
    }

    @PutMapping("/class-schedules/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Sửa lịch lặp đang hoạt động; giữ nguyên mọi buổi đã sinh")
    public Schedule update(Authentication auth, @PathVariable @Positive Long id,
            @Valid @RequestBody ScheduleRequest request) {
        return schedules.save(CurrentUser.from(auth), id, request);
    }

    @PutMapping("/class-schedules/{id}/deactivate")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Ngừng lịch lặp; không xóa hoặc hủy buổi đã sinh")
    public Schedule deactivate(Authentication auth, @PathVariable @Positive Long id) {
        return schedules.deactivate(CurrentUser.from(auth), id);
    }

    @PostMapping("/class-sessions/{id}/bookings")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST')")
    @Operation(summary = "Đặt chỗ khi lớp còn chỗ; chỉ TIME ACTIVE; lễ tân đặt hộ đúng chi nhánh",
        description = "201 cho BOOKED, 409 nếu lớp đã đủ chỗ. createdBy lấy từ JWT.")
    public ResponseEntity<Booking> book(Authentication auth, @PathVariable @Positive Long id,
            @Valid @RequestBody BookingRequest request) {
        var b = bookings.book(CurrentUser.from(auth), id, request.memberId());
        return ResponseEntity.created(URI.create("/api/v1/class-bookings/" + b.id())).body(b);
    }

    @GetMapping("/class-bookings/{id}")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST')")
    @Operation(summary = "Xem chỗ đặt và thời điểm hết khóa đặt lớp")
    public Booking detail(Authentication auth, @PathVariable @Positive Long id) {
        return bookings.detail(CurrentUser.from(auth), id);
    }

    @PutMapping("/class-bookings/{id}/cancel")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST')")
    @Operation(summary = "Hủy chỗ trước giờ học; buổi đã đổi giờ hủy không phạt")
    public Booking cancel(Authentication auth, @PathVariable @Positive Long id) {
        return bookings.cancel(CurrentUser.from(auth), id);
    }

    @PutMapping("/class-sessions/{id}/time")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','SUPER_ADMIN')")
    @Operation(summary = "Đổi giờ buổi học và giữ chỗ đã đặt")
    public Session changeTime(Authentication auth, @PathVariable @Positive Long id,
            @Valid @RequestBody SessionTimeRequest request) {
        return sessions.changeTime(CurrentUser.from(auth), id, request);
    }

    @PutMapping("/class-sessions/{id}/room")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','SUPER_ADMIN')")
    @Operation(summary = "Đổi phòng buổi học và giữ chỗ đã đặt")
    public Session changeRoom(Authentication auth, @PathVariable @Positive Long id,
            @Valid @RequestBody SessionRoomRequest request) {
        return sessions.changeRoom(CurrentUser.from(auth), id, request.roomId());
    }

    @PutMapping("/class-sessions/{id}/cancel")
    @PreAuthorize("hasAnyRole('BRANCH_MANAGER','SUPER_ADMIN')")
    @Operation(summary = "Hủy buổi và các chỗ đang đặt, không ghi vi phạm")
    public Session cancelSession(Authentication auth, @PathVariable @Positive Long id) {
        return sessions.cancel(CurrentUser.from(auth), id);
    }

    @GetMapping("/class-sessions/{id}/attendance")
    @PreAuthorize("hasAnyRole('TRAINER','BRANCH_MANAGER')")
    @Operation(summary = "Xem chỗ đặt và trạng thái check-in để điểm danh")
    public AttendanceRoster roster(Authentication auth, @PathVariable @Positive Long id) {
        return attendance.roster(CurrentUser.from(auth), id);
    }

    @PutMapping("/class-sessions/{id}/attendance")
    @PreAuthorize("hasAnyRole('TRAINER','BRANCH_MANAGER')")
    @Operation(summary = "Điểm danh hoặc sửa kết quả theo phạm vi và thời hạn của vai trò")
    public AttendanceRow markAttendance(Authentication auth, @PathVariable @Positive Long id,
            @Valid @RequestBody AttendanceRequest request) {
        return attendance.mark(CurrentUser.from(auth), id, request);
    }

    @DeleteMapping("/members/{id}/class-booking-lock")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Gỡ khóa đặt lớp của hội viên thuộc chi nhánh và ghi audit")
    public ResponseEntity<Void> unlockClassBooking(Authentication auth, @PathVariable @Positive Long id) {
        attendance.unlock(CurrentUser.from(auth), id);
        return ResponseEntity.noContent().build();
    }
}
