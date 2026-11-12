package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import java.time.OffsetDateTime;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageType;

public record PackageDetailDto(Long id, String code, String name, PackageType type,
        Integer durationDays, Integer sessionCount, Integer price, String description,
        PackageStatus status, Long branchId, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
