package vn.edu.hust.nhom6.gym.feature.branch.repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Chỉ đọc bảng branches; không tạo hoặc cập nhật chi nhánh. */
@Repository
@RequiredArgsConstructor
public class BranchLookupRepository {
    private final EntityManager entities;

    public Optional<Long> findActiveIdByCode(String code) {
        List<?> rows = entities.createNativeQuery("""
            SELECT id FROM branches WHERE code = :code AND status = 'ACTIVE'
            """, Long.class).setParameter("code", code).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of((Long) rows.getFirst());
    }

    public boolean existsActiveById(Long branchId) {
        return !entities.createNativeQuery("""
            SELECT id FROM branches WHERE id = :id AND status = 'ACTIVE'
            """, Long.class).setParameter("id", branchId).getResultList().isEmpty();
    }

    /** Giờ mở và đóng cửa của chi nhánh (cột {@code time}, giờ Việt Nam). */
    public record OpeningHours(LocalTime open, LocalTime close) { }

    /**
     * Đọc {@code open_time}, {@code close_time} dạng chữ {@code HH24:MI:SS} rồi đổi sang {@link LocalTime}. Không đọc qua
     * kiểu {@code time} của JDBC vì cấu hình {@code hibernate.jdbc.time_zone: UTC} làm Hibernate quy đổi giờ theo múi giờ
     * máy năm 1970 (VD-13): giờ có thể lệch vài tiếng, ví dụ {@code 18:30} đọc ra {@code 10:30}.
     */
    public Optional<OpeningHours> findOpeningHours(Long branchId) {
        List<?> rows = entities.createNativeQuery("""
            SELECT to_char(open_time, 'HH24:MI:SS') || ' ' || to_char(close_time, 'HH24:MI:SS')
            FROM branches WHERE id = :id
            """, String.class).setParameter("id", branchId).getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = ((String) rows.getFirst()).split(" ");
        return Optional.of(new OpeningHours(LocalTime.parse(parts[0]), LocalTime.parse(parts[1])));
    }
}
