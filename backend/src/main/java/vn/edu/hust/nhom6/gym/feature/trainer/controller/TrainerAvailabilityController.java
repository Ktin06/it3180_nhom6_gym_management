package vn.edu.hust.nhom6.gym.feature.trainer.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerAvailabilityDto;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerAvailabilityRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerScheduleResponse;
import vn.edu.hust.nhom6.gym.feature.trainer.service.TrainerAvailabilityService;

/**
 * API lịch rảnh và lịch dạy của huấn luyện viên (F9, UC-F9.1) và đọc khung rảnh để hội viên đặt buổi (UC-F9.2).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "F9 – Lịch rảnh và lịch dạy của huấn luyện viên")
@RequiredArgsConstructor
public class TrainerAvailabilityController {
    private final TrainerAvailabilityService availabilities;

    @PostMapping("/pt/availabilities")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "PT khai báo khung giờ rảnh hằng tuần (UC-F9.1)",
        description = "Mã PT lấy từ token qua StaffService, không nhận trainerId từ body. dayOfWeek theo ISO: 1..7.")
    @ApiResponse(responseCode = "201", description = "Đã tạo khung giờ rảnh")
    @ApiResponse(responseCode = "400", description = "Thiếu trường, thứ ngoài 1–7, hoặc giờ kết thúc không sau giờ bắt đầu")
    @ApiResponse(responseCode = "403", description = "Không phải huấn luyện viên hoặc sai chi nhánh làm việc")
    @ApiResponse(responseCode = "422", description = "Khung chồng lên khung đã khai báo của cùng PT cùng thứ (E4)")
    public ResponseEntity<TrainerAvailabilityDto> create(Authentication auth,
            @Valid @RequestBody TrainerAvailabilityRequest request) {
        var result = availabilities.create(CurrentUser.from(auth), request);
        return ResponseEntity.created(URI.create("/api/v1/pt/availabilities/" + result.id())).body(result);
    }

    @PutMapping("/pt/availabilities/{id}")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "PT sửa khung giờ rảnh của chính mình (UC-F9.1)")
    @ApiResponse(responseCode = "200", description = "Đã cập nhật khung giờ rảnh")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải PT hoặc khung thuộc PT khác (E2)")
    @ApiResponse(responseCode = "404", description = "Khung giờ không tồn tại (E3)")
    @ApiResponse(responseCode = "422", description = "Chồng khung cùng PT (E4) hoặc thu hẹp làm buổi BOOKED tương lai rơi ra ngoài (E5)")
    public TrainerAvailabilityDto update(Authentication auth,
            @PathVariable @Positive(message = "Mã khung giờ không hợp lệ.") Long id,
            @Valid @RequestBody TrainerAvailabilityRequest request) {
        return availabilities.update(CurrentUser.from(auth), id, request);
    }

    @DeleteMapping("/pt/availabilities/{id}")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "PT xóa khung giờ rảnh (xóa hẳn dòng, ghi audit_logs; UC-F9.1, BR-F9.15)")
    @ApiResponse(responseCode = "204", description = "Đã xóa khung giờ rảnh")
    @ApiResponse(responseCode = "403", description = "Không phải PT hoặc khung thuộc PT khác (E2)")
    @ApiResponse(responseCode = "404", description = "Khung giờ không tồn tại (E3)")
    @ApiResponse(responseCode = "422", description = "Còn buổi PT BOOKED tương lai nằm trong khung (E5)")
    public ResponseEntity<Void> delete(Authentication auth,
            @PathVariable @Positive(message = "Mã khung giờ không hợp lệ.") Long id) {
        availabilities.delete(CurrentUser.from(auth), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pt/schedule")
    @PreAuthorize("hasRole('TRAINER')")
    @Operation(summary = "PT xem khung rảnh, buổi PT và buổi lớp nhóm được phân công trong khoảng ngày (UC-F9.1)",
        description = "from, to theo ngày Việt Nam (YYYY-MM-DD). Bỏ trống mặc định 7 ngày kể từ hôm nay.")
    @ApiResponse(responseCode = "200", description = "Lịch dạy của huấn luyện viên")
    @ApiResponse(responseCode = "400", description = "Khoảng ngày không hợp lệ (from > to hoặc sai định dạng)")
    @ApiResponse(responseCode = "403", description = "Không phải huấn luyện viên")
    public TrainerScheduleResponse schedule(Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return availabilities.getSchedule(CurrentUser.from(auth), from, to);
    }

    @GetMapping("/trainers/{trainerId}/availabilities")
    @PreAuthorize("hasRole('MEMBER')")
    @Operation(summary = "Hội viên xem khung giờ rảnh của một huấn luyện viên để đặt buổi PT (UC-F9.2)")
    @ApiResponse(responseCode = "200", description = "Danh sách khung rảnh sắp theo thứ và giờ bắt đầu")
    @ApiResponse(responseCode = "400", description = "Mã huấn luyện viên không hợp lệ")
    @ApiResponse(responseCode = "403", description = "Không phải hội viên")
    @ApiResponse(responseCode = "404", description = "Huấn luyện viên không tồn tại hoặc đã ngừng hoạt động")
    public List<TrainerAvailabilityDto> listForTrainer(
            @PathVariable @Positive(message = "Mã huấn luyện viên không hợp lệ.") Long trainerId) {
        return availabilities.listForTrainer(trainerId);
    }
}
