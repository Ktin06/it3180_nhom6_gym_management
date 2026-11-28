package vn.edu.hust.nhom6.gym.feature.trainer.entity;

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
 * Một dòng nhật ký tập: một bài tập của học viên trong một ngày (bảng {@code workout_logs}, UC-F9.5). Không có setter.
 * Bảng không có cột người ghi; dấu vết PT nằm trong {@code audit_logs} (GĐ-11 của S2-20.1).
 */
@Entity
@Table(name = "workout_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutLog extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** Buổi PT {@code DONE} của đúng PT và hội viên; rỗng nếu ghi ngoài buổi PT. */
    @Column(name = "pt_session_id")
    private Long ptSessionId;

    /** Ngày tập theo giờ Việt Nam. */
    @Column(name = "logged_at", nullable = false)
    private LocalDate loggedAt;

    @Column(nullable = false, length = 100)
    private String exercise;

    private Integer sets;

    private Integer reps;

    /** Mức tạ, kg. */
    @Column(precision = 6, scale = 2)
    private BigDecimal weight;

    public static WorkoutLog create(Long memberId, Long ptSessionId, LocalDate loggedAt, String exercise,
            Integer sets, Integer reps, BigDecimal weight) {
        WorkoutLog log = new WorkoutLog();
        log.memberId = memberId;
        log.ptSessionId = ptSessionId;
        log.loggedAt = loggedAt;
        log.exercise = exercise;
        log.sets = sets;
        log.reps = reps;
        log.weight = weight;
        return log;
    }
}
