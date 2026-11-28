package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hust.nhom6.gym.feature.trainer.entity.TrainerAvailability;

public interface TrainerAvailabilityRepository extends JpaRepository<TrainerAvailability, Long> {

    List<TrainerAvailability> findByTrainerIdOrderByDayOfWeekAscStartTimeAscIdAsc(Long trainerId);

    List<TrainerAvailability> findByTrainerIdAndDayOfWeekOrderByStartTimeAscIdAsc(Long trainerId, Integer dayOfWeek);
}
