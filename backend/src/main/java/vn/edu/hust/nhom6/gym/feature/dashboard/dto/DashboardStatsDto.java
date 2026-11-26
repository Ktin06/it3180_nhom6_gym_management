package vn.edu.hust.nhom6.gym.feature.dashboard.dto;

/**
 * Tổng hợp chỉ số KPI trong ngày của một chi nhánh (F6 UC-F6.1, UC-F6.2, UC-F6.3, UC-F6.4).
 * Tiền là số nguyên VND ({@link Integer}, BR-F6.3). Bỏ trường {@code todayCheckinGrowthRate} theo giả định G10.
 */
public record DashboardStatsDto(
        long currentlyInGym,
        long todayCheckinCount,
        Integer todayRevenue,
        Integer cashRevenue,
        Integer transferRevenue,
        long expiringPackagesCount) {
}
