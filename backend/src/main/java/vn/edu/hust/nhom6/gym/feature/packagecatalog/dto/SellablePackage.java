package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Thông tin gói cần khi bán (UC-F3.5), chụp tại thời điểm bán: giá và tên chép vào dòng hóa đơn (BR-F3-13),
 * loại, chi nhánh, số ngày và số buổi chép vào gói hội viên.
 *
 * @param branchId rỗng = gói dùng toàn chuỗi
 * @param sessionCount rỗng với gói TIME
 */
public record SellablePackage(Long id, String name, PackageType type, Long branchId,
        int durationDays, Integer sessionCount, int price) { }
