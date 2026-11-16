package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

/** Hợp đồng danh mục dùng bởi frontend, không trả entity hoặc dữ liệu membership. */
public record PackageSummaryDto(Long id, String code, String name, PackageType type,
        Integer durationDays, Integer sessionCount, Integer price, String description,
        PackageStatus status, Long branchId) {}
