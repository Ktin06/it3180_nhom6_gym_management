package vn.edu.hust.nhom6.gym.feature.dashboard.dto;

/** Một khung giờ trong biểu đồ check-in theo giờ mở cửa chi nhánh (F6 UC-F6.2). */
public record HourlyCheckinDto(
        String hour,
        long count) {
}
