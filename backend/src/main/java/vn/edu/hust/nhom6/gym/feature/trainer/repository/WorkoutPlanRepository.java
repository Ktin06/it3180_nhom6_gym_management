package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hust.nhom6.gym.feature.trainer.entity.WorkoutPlan;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    /** Mọi giáo án của hội viên, mới nhất trước. */
    List<WorkoutPlan> findByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);
}
