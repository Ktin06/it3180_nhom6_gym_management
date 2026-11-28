package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hust.nhom6.gym.feature.trainer.entity.WorkoutLog;

public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {
    /** Nhật ký của hội viên có ngày tập trong khoảng nửa mở {@code [from, toExclusive)}, ngày tăng dần. */
    List<WorkoutLog> findByMemberIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAscIdAsc(
            Long memberId, LocalDate from, LocalDate toExclusive);
}
