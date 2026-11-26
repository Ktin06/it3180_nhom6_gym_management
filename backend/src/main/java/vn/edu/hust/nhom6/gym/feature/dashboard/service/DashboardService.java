package vn.edu.hust.nhom6.gym.feature.dashboard.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;
import vn.edu.hust.nhom6.gym.common.security.CurrentUser;
import vn.edu.hust.nhom6.gym.common.time.VnTime;
import vn.edu.hust.nhom6.gym.feature.branch.repository.BranchLookupRepository.OpeningHours;
import vn.edu.hust.nhom6.gym.feature.branch.service.BranchLookupService;
import vn.edu.hust.nhom6.gym.feature.checkin.dto.CheckinStatus;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.DashboardBranchDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.DashboardStatsDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.ExpiringMembershipDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.ExpiringWarningReason;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.HourlyCheckinDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.dto.LatestCheckinDto;
import vn.edu.hust.nhom6.gym.feature.dashboard.repository.DashboardQueryRepository;
import vn.edu.hust.nhom6.gym.feature.dashboard.repository.DashboardQueryRepository.CheckinStatsRow;
import vn.edu.hust.nhom6.gym.feature.dashboard.repository.DashboardQueryRepository.ExpiringMembershipRow;
import vn.edu.hust.nhom6.gym.feature.dashboard.repository.DashboardQueryRepository.LatestCheckinRow;
import vn.edu.hust.nhom6.gym.feature.dashboard.repository.DashboardQueryRepository.RevenueRow;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;
import vn.edu.hust.nhom6.gym.feature.staff.service.StaffService;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigKeys;
import vn.edu.hust.nhom6.gym.feature.systemconfig.service.SystemConfigService;

/**
 * Nghiệp vụ đọc tổng hợp cho Dashboard cơ bản của chi nhánh (F6 UC-F6.1–UC-F6.5).
 * Nhân viên chỉ thấy chi nhánh của mình ({@link StaffService#requireBranchId}); chủ chuỗi bắt buộc truyền
 * {@code branchId} hợp lệ (thiếu 400, không tồn tại 404). Mọi mốc ngày giờ tính theo giờ Việt Nam qua {@link VnTime}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private static final int EXPIRING_WINDOW_DAYS = 7;
    private static final String OWNER_BRANCH_REQUIRED = "Chủ chuỗi cần chọn chi nhánh cụ thể để xem Dashboard.";

    private final DashboardQueryRepository queries;
    private final StaffService staff;
    private final BranchLookupService branches;
    private final SystemConfigService configs;
    private final VnTime time;

    /** Danh sách chi nhánh (gồm cả {@code INACTIVE} để xem số liệu lịch sử) cho chủ chuỗi chọn trên Dashboard. */
    public List<DashboardBranchDto> branches() {
        return branches.listAll().stream()
            .map(row -> new DashboardBranchDto(row.id(), row.code(), row.name(), row.status()))
            .toList();
    }

    /** Tổng hợp chỉ số KPI trong ngày của chi nhánh (UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4). */
    public DashboardStatsDto stats(CurrentUser me, Long requestedBranchId) {
        Long branchId = resolveBranchId(me, requestedBranchId);
        LocalDate today = time.today();
        OffsetDateTime dayStart = time.dayStart(today);
        OffsetDateTime dayEnd = time.dayEnd(today);
        int sessionThreshold = configs.getInt(branchId, SystemConfigKeys.SESSION_WARNING_THRESHOLD);

        CheckinStatsRow checkinStats = queries.countCheckinStats(branchId, dayStart, dayEnd);
        RevenueRow revenue = queries.sumTodayRevenue(branchId, dayStart, dayEnd);
        long expiringCount = queries.countExpiringMemberships(
            branchId, today, today.plusDays(EXPIRING_WINDOW_DAYS), sessionThreshold);

        return new DashboardStatsDto(
            checkinStats.currentlyInGym(),
            checkinStats.todayCheckinCount(),
            revenue.totalRevenue(),
            revenue.cashRevenue(),
            revenue.transferRevenue(),
            expiringCount);
    }

    /**
     * Biểu đồ số lượt check-in theo từng khung giờ trong giờ mở cửa của chi nhánh (UC-F6.2).
     * Giờ mở cửa đọc dạng chữ qua {@link BranchLookupService#openingHours(Long)} (VD-13). Khung trống điền 0.
     */
    public List<HourlyCheckinDto> hourlyCheckins(CurrentUser me, Long requestedBranchId) {
        BranchScope scope = resolveBranchWithHours(me, requestedBranchId);
        LocalDate today = time.today();
        Map<Integer, Long> countsByHour = queries.findHourlyCheckinCounts(
            scope.branchId(), time.dayStart(today), time.dayEnd(today));

        int startHour = scope.hours().open().getHour();
        // Đúng giờ đóng cửa là ngoài giờ (F4), nên lùi 1 nano để lấy giờ chứa phút cuối còn mở cửa (VD: 22:00 -> 21h)
        int endHour = scope.hours().close().minusNanos(1).getHour();

        List<HourlyCheckinDto> result = new ArrayList<>(Math.max(0, endHour - startHour + 1));
        for (int hour = startHour; hour <= endHour; hour++) {
            result.add(new HourlyCheckinDto(String.format("%02d:00", hour), countsByHour.getOrDefault(hour, 0L)));
        }
        return result;
    }

    /** Danh sách cảnh báo gói tập sắp hết hạn trong 7 ngày tới hoặc gói SESSION sắp hết buổi (UC-F6.4, BR-F6.5). */
    public List<ExpiringMembershipDto> expiringMemberships(CurrentUser me, Long requestedBranchId, int limit) {
        Long branchId = resolveBranchId(me, requestedBranchId);
        LocalDate today = time.today();
        LocalDate expireBefore = today.plusDays(EXPIRING_WINDOW_DAYS);
        int sessionThreshold = configs.getInt(branchId, SystemConfigKeys.SESSION_WARNING_THRESHOLD);

        return queries.findExpiringMemberships(branchId, today, expireBefore, sessionThreshold, limit).stream()
            .map(row -> toExpiringDto(row, today, expireBefore))
            .toList();
    }

    /** Danh sách lượt check-in mới nhất trong ngày tại chi nhánh (UC-F6.5), trả {@code hasAvatar} (ADR 0011). */
    public List<LatestCheckinDto> latestCheckins(CurrentUser me, Long requestedBranchId, int limit) {
        Long branchId = resolveBranchId(me, requestedBranchId);
        LocalDate today = time.today();

        return queries.findLatestCheckins(branchId, time.dayStart(today), time.dayEnd(today), limit).stream()
            .map(DashboardService::toLatestCheckinDto)
            .toList();
    }

    private Long resolveBranchId(CurrentUser me, Long requestedBranchId) {
        if (!me.isOwner()) {
            return staff.requireBranchId(me.userId());
        }
        if (requestedBranchId == null) {
            throw new InvalidRequestException(OWNER_BRANCH_REQUIRED);
        }
        // Kiểm tra chi nhánh tồn tại trong branches (ném 404 "Không tìm thấy chi nhánh." nếu không có)
        branches.openingHours(requestedBranchId);
        return requestedBranchId;
    }

    private BranchScope resolveBranchWithHours(CurrentUser me, Long requestedBranchId) {
        Long branchId;
        if (!me.isOwner()) {
            branchId = staff.requireBranchId(me.userId());
        } else {
            if (requestedBranchId == null) {
                throw new InvalidRequestException(OWNER_BRANCH_REQUIRED);
            }
            branchId = requestedBranchId;
        }
        return new BranchScope(branchId, branches.openingHours(branchId));
    }

    private static ExpiringMembershipDto toExpiringDto(ExpiringMembershipRow row, LocalDate today,
            LocalDate expireBefore) {
        long daysRemaining = ChronoUnit.DAYS.between(today, row.endDate());
        Integer remainingSessions = row.packageType() == PackageType.SESSION ? row.remainingSessions() : null;
        ExpiringWarningReason reason = warningReason(row, today, expireBefore);
        return new ExpiringMembershipDto(
            row.membershipId(),
            row.memberId(),
            row.memberCode(),
            row.fullName(),
            row.phone(),
            row.hasAvatar(),
            row.packageName(),
            row.packageType(),
            row.endDate(),
            daysRemaining,
            remainingSessions,
            reason);
    }

    private static ExpiringWarningReason warningReason(ExpiringMembershipRow row, LocalDate today,
            LocalDate expireBefore) {
        if (row.packageType() == PackageType.SESSION
                && row.remainingSessions() != null
                && row.remainingSessions() == 0) {
            return ExpiringWarningReason.LOW_SESSIONS;
        }
        if (!row.endDate().isBefore(today) && row.endDate().isBefore(expireBefore)) {
            return ExpiringWarningReason.EXPIRING_DATE;
        }
        return ExpiringWarningReason.LOW_SESSIONS;
    }

    private static LatestCheckinDto toLatestCheckinDto(LatestCheckinRow row) {
        Long durationMinutes = row.checkedOutAt() == null
            ? null
            : Math.max(0L, Duration.between(row.checkedInAt(), row.checkedOutAt()).toMinutes());
        CheckinStatus status = row.checkedOutAt() == null ? CheckinStatus.CHECKED_IN : CheckinStatus.CHECKED_OUT;
        return new LatestCheckinDto(
            row.checkinId(),
            row.memberId(),
            row.memberCode(),
            row.fullName(),
            row.hasAvatar(),
            row.packageName(),
            row.packageType(),
            row.method(),
            row.checkedInAt(),
            row.checkedOutAt(),
            durationMinutes,
            status);
    }

    private record BranchScope(Long branchId, OpeningHours hours) { }
}
