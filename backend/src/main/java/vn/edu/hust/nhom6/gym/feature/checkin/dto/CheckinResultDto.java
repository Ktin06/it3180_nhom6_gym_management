package vn.edu.hust.nhom6.gym.feature.checkin.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Kết quả check-in thành công (UC-F4.2 bước 9, SRS F4 mục 6.2). Thêm so với bản nháp: {@code checkinDate} (ngày Việt
 * Nam), {@code appliedMembership.packageName} và {@code classArrivals} (các lớp vừa được ghi đã đến, UC-F4.3 bước 3).
 */
public record CheckinResultDto(Long checkinId, Long memberId, String memberCode, String fullName,
        AppliedMembership appliedMembership, LocalDate checkinDate, OffsetDateTime checkedInAt,
        boolean classArrivalMarked, List<ClassArrivalDto> classArrivals, String message) {

    public static final String SUCCESS = "Check-in thành công. Chúc quý hội viên buổi tập hiệu quả!";

    /** Gói đã dùng; {@code remainingSessions} rỗng với gói TIME (không có trong JSON). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AppliedMembership(Long id, String packageName, PackageType packageType, boolean sessionDeducted,
            Integer remainingSessions) { }
}
