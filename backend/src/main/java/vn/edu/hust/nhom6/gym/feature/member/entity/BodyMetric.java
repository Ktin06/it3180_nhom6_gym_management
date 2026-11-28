package vn.edu.hust.nhom6.gym.feature.member.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/**
 * Một lần đo chỉ số cơ thể của hội viên (bảng {@code body_metrics}, UC-F2.5, UC-F9.5). Mỗi lần đo một dòng, không sửa,
 * không xóa. {@code recorded_by} là {@code users.id} người ghi, lấy từ token.
 */
@Entity
@Table(name = "body_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BodyMetric extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "recorded_by")
    private Long recordedBy;

    /** Ngày đo theo giờ Việt Nam. */
    @Column(name = "measured_at", nullable = false)
    private LocalDate measuredAt;

    /** kg. */
    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    /** cm. */
    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    /** %. */
    @Column(name = "body_fat", precision = 4, scale = 2)
    private BigDecimal bodyFat;

    /** kg. */
    @Column(name = "muscle_mass", precision = 5, scale = 2)
    private BigDecimal muscleMass;

    private String note;

    public static BodyMetric record(Long memberId, Long recordedBy, LocalDate measuredAt, BigDecimal weight,
            BigDecimal height, BigDecimal bodyFat, BigDecimal muscleMass, String note) {
        BodyMetric metric = new BodyMetric();
        metric.memberId = memberId;
        metric.recordedBy = recordedBy;
        metric.measuredAt = measuredAt;
        metric.weight = weight;
        metric.height = height;
        metric.bodyFat = bodyFat;
        metric.muscleMass = muscleMass;
        metric.note = note;
        return metric;
    }
}
