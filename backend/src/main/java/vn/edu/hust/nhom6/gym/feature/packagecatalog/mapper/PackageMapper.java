package vn.edu.hust.nhom6.gym.feature.packagecatalog.mapper;

import java.time.ZoneOffset;

import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageSummaryDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageDetailDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.dto.PackageDeactivationDto;
import vn.edu.hust.nhom6.gym.feature.packagecatalog.entity.GymPackage;

public final class PackageMapper {
    private PackageMapper() {}

    public static PackageDeactivationDto deactivation(GymPackage item) {
        return new PackageDeactivationDto(item.getId(), item.getCode(), item.getStatus(),
            item.getUpdatedAt().withOffsetSameInstant(ZoneOffset.UTC));
    }

    public static PackageDetailDto detail(GymPackage item) {
        return new PackageDetailDto(item.getId(), item.getCode(), item.getName(), item.getType(),
            item.getDurationDays(), item.getSessionCount(), item.getPrice(), item.getDescription(),
            item.getStatus(), item.getBranchId(), item.getCreatedAt().withOffsetSameInstant(ZoneOffset.UTC),
            item.getUpdatedAt().withOffsetSameInstant(ZoneOffset.UTC));
    }

    public static PackageSummaryDto summary(GymPackage item) {
        return new PackageSummaryDto(item.getId(), item.getCode(), item.getName(), item.getType(),
            item.getDurationDays(), item.getSessionCount(), item.getPrice(), item.getDescription(),
            item.getStatus(), item.getBranchId());
    }
}
