package vn.edu.hust.nhom6.gym.feature.trainer.entity;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;

/**
 * Giáo án PT soạn cho học viên (bảng {@code workout_plans}, UC-F9.4). Một hội viên có nhiều giáo án theo thời gian
 * (GĐ-3 của S2-20.1); chỉ thêm dòng mới, không có setter.
 *
 * <p>{@code content} là chuỗi JSON (cột {@code jsonb}); service chuyển qua lại với danh sách buổi tập bằng
 * {@code JsonMapper}, để entity không phụ thuộc thư viện JSON mà Hibernate tự chọn.
 */
@Entity
@Table(name = "workout_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkoutPlan extends BaseEntity {

    /** {@code staff.id} của PT soạn giáo án. */
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String content;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public static WorkoutPlan create(Long trainerId, Long memberId, String title, String contentJson,
            LocalDate startDate, LocalDate endDate) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.trainerId = trainerId;
        plan.memberId = memberId;
        plan.title = title;
        plan.content = contentJson;
        plan.startDate = startDate;
        plan.endDate = endDate;
        return plan;
    }
}
