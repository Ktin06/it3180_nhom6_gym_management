package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/**
 * Một gói trên bảng giá công khai cho khách (US-F3.1.2).
 *
 * <p>Chỉ gồm các trường cần hiển thị. Không có mã nội bộ, trạng thái, chi nhánh hay thời gian tạo (AC-F3.1.2-03).
 */
public record PublicPackageDto(Long id, String name, PackageType type, Integer durationDays,
        Integer sessionCount, Integer price, String description) {}
