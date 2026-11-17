package vn.edu.hust.nhom6.gym.feature.packagecatalog.dto;

import java.time.OffsetDateTime;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.PackageStatus;

/** Response ngừng bán theo UC-F3.4, không phải trạng thái membership. */
public record PackageDeactivationDto(Long id, String code, PackageStatus status, OffsetDateTime updatedAt) {}
