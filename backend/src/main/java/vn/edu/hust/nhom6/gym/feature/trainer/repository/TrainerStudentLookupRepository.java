package vn.edu.hust.nhom6.gym.feature.trainer.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.trainer.dto.TrainerStudentDto;

/**
 * Truy vấn <b>chỉ đọc</b>, có tham số, để biết huấn luyện viên có dạy hội viên không (BR-F9.10; G4). Đọc
 * {@code members}, {@code staff}, {@code pt_sessions}; không ghi gì.
 *
 * <p>"Dạy" (VD-26 vấn đề 1, SUA-5): chỉ tính buổi PT chưa hủy ({@code BOOKED}, {@code DONE}, {@code NO_SHOW}) giữa
 * huấn luyện viên và hội viên; không tính lớp học nhóm (huấn luyện viên lớp nhóm không xem chỉ số cơ thể hay giáo án
 * của học viên lớp). Vẫn tính buổi {@code BOOKED} để PT soạn giáo án trước buổi đầu. Chưa giới hạn thời gian.
 * {@link #teaches} và {@link #findStudents} dùng <b>cùng</b> điều kiện, để danh sách và kiểm tra quyền không lệch.
 */
@Repository
@RequiredArgsConstructor
public class TrainerStudentLookupRepository {
    private final EntityManager entities;

    /** Hội viên chưa xóa mềm: id, chi nhánh gốc, mã, họ tên. */
    public record MemberRow(Long id, Long homeBranchId, String memberCode, String fullName) { }

    /** Buổi PT (chỉ đọc) phục vụ kiểm tra khi ghi nhật ký tập (UC-F9.5). */
    public record PtSessionRow(Long trainerId, Long memberId, String status, OffsetDateTime startAt) { }

    private static final String TAUGHT_BY_PT = """
        SELECT 1 FROM pt_sessions p
        WHERE p.trainer_id = :trainerId AND p.member_id = %s AND p.status <> 'CANCELLED'""";

    public Optional<MemberRow> findMember(Long memberId) {
        List<Object[]> rows = rows(native_("""
            SELECT id, home_branch_id, member_code, full_name
            FROM members WHERE id = :id AND deleted_at IS NULL""")
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("home_branch_id", StandardBasicTypes.LONG)
            .addScalar("member_code", StandardBasicTypes.STRING)
            .addScalar("full_name", StandardBasicTypes.STRING)
            .setParameter("id", memberId));
        return rows.stream().findFirst()
            .map(r -> new MemberRow((Long) r[0], (Long) r[1], (String) r[2], (String) r[3]));
    }

    /** {@code staff.id} của dòng {@code staff} ACTIVE gắn với {@code users.id}. */
    public Optional<Long> findActiveStaffIdByUserId(Long userId) {
        List<?> rows = entities.createNativeQuery(
            "SELECT id FROM staff WHERE user_id = :userId AND status = 'ACTIVE'")
            .setParameter("userId", userId)
            .getResultList();
        return rows.stream().findFirst().map(v -> ((Number) v).longValue());
    }

    /** Chi nhánh làm việc của huấn luyện viên theo {@code staff.id}. */
    public Optional<Long> findStaffBranchId(Long staffId) {
        List<?> rows = entities.createNativeQuery("SELECT branch_id FROM staff WHERE id = :id")
            .setParameter("id", staffId)
            .getResultList();
        return rows.stream().findFirst().map(v -> ((Number) v).longValue());
    }

    /** Buổi PT theo id (chỉ đọc) để kiểm tra khi ghi nhật ký tập (UC-F9.5). */
    public Optional<PtSessionRow> findPtSession(Long sessionId) {
        List<Object[]> rows = rows(native_("""
            SELECT trainer_id, member_id, status, start_at
            FROM pt_sessions WHERE id = :id""")
            .addScalar("trainer_id", StandardBasicTypes.LONG)
            .addScalar("member_id", StandardBasicTypes.LONG)
            .addScalar("status", StandardBasicTypes.STRING)
            .addScalar("start_at", StandardBasicTypes.OFFSET_DATE_TIME)
            .setParameter("id", sessionId));
        return rows.stream().findFirst()
            .map(r -> new PtSessionRow((Long) r[0], (Long) r[1], (String) r[2], (OffsetDateTime) r[3]));
    }

    /** Họ tên huấn luyện viên theo {@code staff.id}; rỗng nếu không có. */
    public Optional<String> findStaffName(Long staffId) {
        List<?> rows = entities.createNativeQuery("SELECT full_name FROM staff WHERE id = :id")
            .setParameter("id", staffId)
            .getResultList();
        return rows.stream().findFirst().map(String::valueOf);
    }

    public boolean teaches(Long trainerId, Long memberId) {
        Object found = entities.createNativeQuery("SELECT EXISTS (" + TAUGHT_BY_PT.formatted(":memberId") + ")")
            .setParameter("trainerId", trainerId)
            .setParameter("memberId", memberId)
            .getSingleResult();
        return Boolean.TRUE.equals(found);
    }

    /**
     * Học viên PT của huấn luyện viên (chưa xóa mềm), sắp theo họ tên rồi id. Trường {@code viaClass} giữ lại để
     * tương thích giao diện S2-20.2 và luôn trả {@code false} (VD-26 vấn đề 1, SUA-5).
     */
    public List<TrainerStudentDto> findStudents(Long trainerId) {
        List<Object[]> rows = rows(native_("""
            SELECT m.id, m.member_code, m.full_name, m.home_branch_id
            FROM members m
            WHERE m.deleted_at IS NULL
              AND EXISTS (%s)
            ORDER BY m.full_name, m.id""".formatted(TAUGHT_BY_PT.formatted("m.id")))
            .addScalar("id", StandardBasicTypes.LONG)
            .addScalar("member_code", StandardBasicTypes.STRING)
            .addScalar("full_name", StandardBasicTypes.STRING)
            .addScalar("home_branch_id", StandardBasicTypes.LONG)
            .setParameter("trainerId", trainerId));
        return rows.stream().map(r -> new TrainerStudentDto((Long) r[0], (String) r[1], (String) r[2], (Long) r[3],
            true, false)).toList();
    }

    @SuppressWarnings("unchecked")
    private NativeQuery<Object[]> native_(String sql) {
        return entities.createNativeQuery(sql).unwrap(NativeQuery.class);
    }

    private static List<Object[]> rows(NativeQuery<Object[]> query) {
        return query.getResultList();
    }
}
