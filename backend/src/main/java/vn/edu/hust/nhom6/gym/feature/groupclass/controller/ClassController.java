package vn.edu.hust.nhom6.gym.feature.groupclass.controller;

import java.net.URI;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import vn.edu.hust.nhom6.gym.common.dto.PageResponse;
import vn.edu.hust.nhom6.gym.feature.groupclass.dto.ClassDtos.*;
import vn.edu.hust.nhom6.gym.feature.groupclass.service.*;
import vn.edu.hust.nhom6.gym.feature.member.service.MemberActor;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "F8 – Lịch lớp và đặt chỗ")
@RequiredArgsConstructor
public class ClassController {
    private final ClassScheduleService schedules;
    private final ClassBookingService bookings;

    @GetMapping("/class-schedules")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Danh sách lịch lặp tại chi nhánh quản lý; page từ 0, size tối đa 100")
    public PageResponse<Schedule> schedules(Authentication auth,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return schedules.list(MemberActor.from(auth), page, size);
    }

    @PostMapping("/class-schedules")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Tạo lịch lặp; thứ ISO 1–7, giờ Việt Nam; chi nhánh lấy từ nhân viên")
    public ResponseEntity<Schedule> create(Authentication auth, @Valid @RequestBody ScheduleRequest request) {
        var s = schedules.save(MemberActor.from(auth), null, request);
        return ResponseEntity.created(URI.create("/api/v1/class-schedules/" + s.id())).body(s);
    }

    @GetMapping("/class-schedules/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Xem lịch lặp trong chi nhánh quản lý")
    public Schedule scheduleDetail(Authentication auth, @PathVariable @Positive Long id) {
        return schedules.detail(MemberActor.from(auth), id);
    }

    @PutMapping("/class-schedules/{id}")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Sửa lịch lặp đang hoạt động; giữ nguyên mọi buổi đã sinh")
    public Schedule update(Authentication auth, @PathVariable @Positive Long id,
            @Valid @RequestBody ScheduleRequest request) {
        return schedules.save(MemberActor.from(auth), id, request);
    }

    @PutMapping("/class-schedules/{id}/deactivate")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    @Operation(summary = "Ngừng lịch lặp; không xóa hoặc hủy buổi đã sinh")
    public Schedule deactivate(Authentication auth, @PathVariable @Positive Long id) {
        return schedules.deactivate(MemberActor.from(auth), id);
    }

    @PostMapping("/class-sessions/{id}/bookings")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST')")
    @Operation(summary = "Đặt chỗ hoặc vào danh sách chờ; chỉ TIME ACTIVE; lễ tân đặt hộ đúng chi nhánh",
        description = "201 cho cả BOOKED và WAITLIST. Xét FIFO trước người mới; createdBy lấy từ JWT.")
    public ResponseEntity<Booking> book(Authentication auth, @PathVariable @Positive Long id,
            @Valid @RequestBody BookingRequest request) {
        var b = bookings.book(MemberActor.from(auth), id, request.memberId());
        return ResponseEntity.created(URI.create("/api/v1/class-bookings/" + b.id())).body(b);
    }

    @GetMapping("/class-bookings/{id}")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST')")
    @Operation(summary = "Xem chỗ đặt, vị trí FIFO hiện tại và thời điểm hết khóa đặt lớp")
    public Booking detail(Authentication auth, @PathVariable @Positive Long id) {
        return bookings.detail(MemberActor.from(auth), id);
    }

    @PutMapping("/class-bookings/{id}/cancel")
    @PreAuthorize("hasAnyRole('MEMBER','RECEPTIONIST')")
    @Operation(summary = "Hủy chỗ trước giờ học; trả isLateCancel và bookingLockedUntil; rời chờ không phạt")
    public Booking cancel(Authentication auth, @PathVariable @Positive Long id) {
        return bookings.cancel(MemberActor.from(auth), id);
    }
}
