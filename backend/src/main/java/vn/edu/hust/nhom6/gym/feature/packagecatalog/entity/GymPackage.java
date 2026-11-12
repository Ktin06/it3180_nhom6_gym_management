package vn.edu.hust.nhom6.gym.feature.packagecatalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/** Ánh xạ danh mục packages theo schema chuẩn #101. */
@Entity
@Table(name = "packages")
@Getter
public class GymPackage extends BaseEntity {
    public void deactivate() { status = PackageStatus.INACTIVE; }

    /** Chỉ cập nhật các thông tin được phép; code/type/branch/status luôn giữ nguyên. */
    public void updateDetails(String name, Integer durationDays, Integer sessionCount, Integer price, String description) {
        this.name = name;
        this.durationDays = durationDays;
        this.sessionCount = sessionCount;
        this.price = price;
        this.description = description;
    }

    protected GymPackage() {}

    public static GymPackage create(Long branchId, String code, String name, PackageType type,
            Integer durationDays, Integer sessionCount, Integer price, String description) {
        GymPackage item = new GymPackage();
        item.branchId = branchId;
        item.code = code;
        item.name = name;
        item.type = type;
        item.durationDays = durationDays;
        item.sessionCount = sessionCount;
        item.price = price;
        item.description = description;
        item.status = PackageStatus.ACTIVE;
        return item;
    }

    @Column(name = "branch_id")
    private Long branchId;
    @Column(nullable = false, length = 30, unique = true)
    private String code;
    @Column(nullable = false, length = 255)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PackageType type;
    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;
    @Column(name = "session_count")
    private Integer sessionCount;
    @Column(nullable = false)
    private Integer price;
    @Column(columnDefinition = "text")
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PackageStatus status;
}
