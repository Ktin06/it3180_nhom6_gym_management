package vn.edu.hust.nhom6.gym.feature.trainer.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerStudentDto;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutLogDto;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutLogRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutPlanDto;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.WorkoutPlanRequest;
import vn.edu.hust.nhom6.gym.feature.trainer.service.TrainerStudentAccess;
import vn.edu.hust.nhom6.gym.feature.trainer.service.WorkoutLogService;
import vn.edu.hust.nhom6.gym.feature.trainer.service.WorkoutPlanService;

/**
 * API của huấn luyện viên với học viên mình dạy (F9, UC-F9.4, UC-F9.5; S2-20.1). PT lấy từ token; chỉ thao tác với
 * học viên mình dạy (BR-F9.10): không dạy mà cùng chi nhánh trả 403, khác chi nhánh hoặc không tồn tại trả 404.
 * Tạo mới trả 201 kèm body, không gửi {@code Location} vì chưa có API xem một giáo án hay một dòng nhật ký theo id.
 */
@RestController
@RequestMapping("/api/v1/pt")
@Tag(name = "F9 – Giáo án và nhật ký tập")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')")
public class TrainerStudentController {
    private final TrainerStudentAccess access;
    private final WorkoutPlanService plans;
    private final WorkoutLogService logs;

    @GetMapping("/students")
    @Operation(summary = "Danh sách học viên PT đang dạy (có buổi PT chưa hủy)")
    @ApiResponse(responseCode = "200", description = "Danh sách học viên, có thể rỗng")
    @ApiResponse(responseCode = "403", description = "Không phải huấn luyện viên đang hoạt động")
    public List<TrainerStudentDto> students(Authentication auth) {
        return access.myStudents(CurrentUser.from(auth));
    }

    @PostMapping("/members/{memberId}/workout-plans")
    @Operation(summary = "Soạn giáo án cho học viên (UC-F9.4)")
    @ApiResponse(responseCode = "201", description = "Đã lưu giáo án")
    @ApiResponse(responseCode = "400", description = "Thiếu tên, nội dung rỗng, sai định dạng, ngày kết thúc trước ngày bắt đầu")
    @ApiResponse(responseCode = "403", description = "PT không dạy hội viên này (E2)")
    @ApiResponse(responseCode = "404", description = "Hội viên không tồn tại hoặc ngoài chi nhánh (E3)")
    public ResponseEntity<WorkoutPlanDto> createPlan(Authentication auth,
            @PathVariable @Positive(message = "ID hội viên phải lớn hơn 0") Long memberId,
            @Valid @RequestBody WorkoutPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plans.create(CurrentUser.from(auth), memberId, request));
    }

    @GetMapping("/members/{memberId}/workout-plans")
    @Operation(summary = "Xem các giáo án của học viên, mới nhất trước")
    @ApiResponse(responseCode = "200", description = "Danh sách giáo án, có thể rỗng")
    @ApiResponse(responseCode = "403", description = "PT không dạy hội viên này")
    @ApiResponse(responseCode = "404", description = "Hội viên không tồn tại hoặc ngoài chi nhánh")
    public List<WorkoutPlanDto> listPlans(Authentication auth,
            @PathVariable @Positive(message = "ID hội viên phải lớn hơn 0") Long memberId) {
        return plans.list(CurrentUser.from(auth), memberId);
    }

    @PostMapping("/members/{memberId}/workout-logs")
    @Operation(summary = "Ghi một dòng nhật ký tập cho học viên (UC-F9.5)",
        description = "ptSessionId tùy chọn: có thì phải là buổi DONE của đúng PT, đúng hội viên, cùng ngày tập.")
    @ApiResponse(responseCode = "201", description = "Đã lưu nhật ký")
    @ApiResponse(responseCode = "400", description = "Thiếu trường, giá trị ngoài khoảng, ngày tập sau hôm nay (E1)")
    @ApiResponse(responseCode = "403", description = "PT không dạy hội viên này (E2)")
    @ApiResponse(responseCode = "404", description = "Hội viên không tồn tại hoặc ngoài chi nhánh (E3)")
    @ApiResponse(responseCode = "422", description = "Buổi PT không thuộc PT và hội viên này, chưa DONE, hoặc khác ngày")
    public ResponseEntity<WorkoutLogDto> createLog(Authentication auth,
            @PathVariable @Positive(message = "ID hội viên phải lớn hơn 0") Long memberId,
            @Valid @RequestBody WorkoutLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logs.create(CurrentUser.from(auth), memberId, request));
    }
}
