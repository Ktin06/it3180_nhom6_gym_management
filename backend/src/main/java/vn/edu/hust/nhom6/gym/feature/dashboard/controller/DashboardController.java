package vn.edu.hust.nhom6.gym.feature.dashboard.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.DashboardBranchDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.DashboardStatsDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.ExpiringMembershipDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.HourlyCheckinDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.LatestCheckinDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.service.DashboardService;

/**
 * Dashboard cơ bản theo chi nhánh (F6). Lễ tân và quản lý chỉ xem chi nhánh mình (tham số {@code branchId} truyền lên
 * bị bỏ qua); chủ chuỗi bắt buộc truyền {@code branchId} (thiếu 400, không tồn tại 404). Hội viên và PT bị 403.
 */
@RestController
@Validated
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('RECEPTIONIST', 'BRANCH_MANAGER', 'SUPER_ADMIN')")
@Tag(name = "F6 – Dashboard cơ bản")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboard;

    @GetMapping("/branches")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Danh sách chi nhánh để chủ chuỗi chọn trên Dashboard (UC-F6.1)",
        description = "Chỉ chủ chuỗi được xem toàn bộ chi nhánh (gồm cả INACTIVE để xem số liệu lịch sử).")
    @ApiResponse(responseCode = "200", description = "Danh sách chi nhánh theo thứ tự id")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải chủ chuỗi")
    public List<DashboardBranchDto> branches() {
        return dashboard.branches();
    }

    @GetMapping("/stats")
    @Operation(summary = "Tổng hợp chỉ số KPI trong ngày của chi nhánh (UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4)",
        description = "Số đang tập, tổng lượt check-in hôm nay, doanh thu hôm nay (tách tiền mặt và chuyển khoản, gồm "
            + "hóa đơn khách lẻ), số gói sắp hết hạn hoặc sắp hết buổi.")
    @ApiResponse(responseCode = "200", description = "Các chỉ số KPI trong ngày")
    @ApiResponse(responseCode = "400", description = "Chủ chuỗi thiếu branchId hoặc branchId không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân, quản lý chi nhánh hoặc chủ chuỗi")
    @ApiResponse(responseCode = "404", description = "Chi nhánh không tồn tại")
    public DashboardStatsDto stats(Authentication auth,
            @RequestParam(required = false) @Positive(message = "ID chi nhánh phải lớn hơn 0.") Long branchId) {
        return dashboard.stats(CurrentUser.from(auth), branchId);
    }

    @GetMapping("/hourly-checkins")
    @Operation(summary = "Biểu đồ số lượt check-in theo khung giờ mở cửa chi nhánh (UC-F6.2)",
        description = "Đủ khung giờ từ giờ mở cửa đến trước giờ đóng cửa theo ngày Việt Nam; khung trống có count = 0.")
    @ApiResponse(responseCode = "200", description = "Danh sách khung giờ và số lượt check-in")
    @ApiResponse(responseCode = "400", description = "Chủ chuỗi thiếu branchId hoặc branchId không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân, quản lý chi nhánh hoặc chủ chuỗi")
    @ApiResponse(responseCode = "404", description = "Chi nhánh không tồn tại")
    public List<HourlyCheckinDto> hourlyCheckins(Authentication auth,
            @RequestParam(required = false) @Positive(message = "ID chi nhánh phải lớn hơn 0.") Long branchId) {
        return dashboard.hourlyCheckins(CurrentUser.from(auth), branchId);
    }

    @GetMapping("/expiring-memberships")
    @Operation(summary = "Danh sách cảnh báo gói tập sắp hết hạn hoặc sắp hết buổi (UC-F6.4)",
        description = "Lọc gói ACTIVE hết hạn trong [today, today+7) hoặc gói SESSION còn <= SESSION_WARNING_THRESHOLD; "
            + "bỏ gói đã có UPCOMING cùng loại và phạm vi; gói toàn chuỗi chỉ hiện ở chi nhánh gốc của hội viên.")
    @ApiResponse(responseCode = "200", description = "Danh sách gói cần cảnh báo")
    @ApiResponse(responseCode = "400", description = "Tham số branchId hoặc limit không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân, quản lý chi nhánh hoặc chủ chuỗi")
    @ApiResponse(responseCode = "404", description = "Chi nhánh không tồn tại")
    public List<ExpiringMembershipDto> expiringMemberships(Authentication auth,
            @RequestParam(required = false) @Positive(message = "ID chi nhánh phải lớn hơn 0.") Long branchId,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Giới hạn phải từ 1 đến 100.")
            @Max(value = 100, message = "Giới hạn phải từ 1 đến 100.") int limit) {
        return dashboard.expiringMemberships(CurrentUser.from(auth), branchId, limit);
    }

    @GetMapping("/latest-checkins")
    @Operation(summary = "Danh sách lượt check-in mới nhất trong ngày (UC-F6.5)",
        description = "Mặc định 10 lượt mới nhất hôm nay theo giờ Việt Nam, có hasAvatar (không có avatarUrl, ADR 0011).")
    @ApiResponse(responseCode = "200", description = "Danh sách lượt check-in mới nhất")
    @ApiResponse(responseCode = "400", description = "Tham số branchId hoặc limit không hợp lệ")
    @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc JWT sai")
    @ApiResponse(responseCode = "403", description = "Không phải lễ tân, quản lý chi nhánh hoặc chủ chuỗi")
    @ApiResponse(responseCode = "404", description = "Chi nhánh không tồn tại")
    public List<LatestCheckinDto> latestCheckins(Authentication auth,
            @RequestParam(required = false) @Positive(message = "ID chi nhánh phải lớn hơn 0.") Long branchId,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Giới hạn phải từ 1 đến 50.")
            @Max(value = 50, message = "Giới hạn phải từ 1 đến 50.") int limit) {
        return dashboard.latestCheckins(CurrentUser.from(auth), branchId, limit);
    }
}
