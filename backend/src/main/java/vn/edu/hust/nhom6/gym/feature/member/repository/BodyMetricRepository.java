package vn.edu.hust.nhom6.gym.feature.member.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.hust.nhom6.gym.feature.member.entity.BodyMetric;

public interface BodyMetricRepository extends JpaRepository<BodyMetric, Long> {
    /** Lịch sử đo, mới nhất trước (UC-F2.5 bước 3). */
    List<BodyMetric> findByMemberIdOrderByMeasuredAtDescIdDesc(Long memberId);

    /** Các lần đo trong khoảng nửa mở {@code [from, toExclusive)}, cũ trước, cho biểu đồ tiến độ. */
    List<BodyMetric> findByMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtAscIdAsc(
            Long memberId, LocalDate from, LocalDate toExclusive);
}
