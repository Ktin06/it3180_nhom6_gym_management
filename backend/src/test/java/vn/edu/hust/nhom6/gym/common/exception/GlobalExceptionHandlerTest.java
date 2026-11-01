package vn.edu.hust.nhom6.gym.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** Kiểm thử đơn vị, không cần Spring hay cơ sở dữ liệu. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundTra404KemThongDiep() {
        ProblemDetail problem = handler.handleNotFound(new NotFoundException("Không tìm thấy hội viên"));

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Không tìm thấy");
        assertThat(problem.getDetail()).isEqualTo("Không tìm thấy hội viên");
    }

    @Test
    void conflictTra409() {
        ProblemDetail problem = handler.handleConflict(new ConflictException("Số điện thoại đã tồn tại"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).isEqualTo("Số điện thoại đã tồn tại");
    }

    @Test
    void viPhamQuyTacNghiepVuTra422() {
        ProblemDetail problem = handler.handleBusinessRule(new BusinessRuleException("Gói PT không dùng để check-in"));

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getTitle()).isEqualTo("Vi phạm quy tắc nghiệp vụ");
    }

    @Test
    void haiNguoiCungSuaMotDongTra409() {
        ProblemDetail problem = handler.handleOptimisticLock(
            new ObjectOptimisticLockingFailureException("Invoice", 1L));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Xung đột dữ liệu");
    }

    @Test
    void viPhamRangBuocCoSoDuLieuTra409KhongLoTenRangBuoc() {
        ProblemDetail problem = handler.handleDataIntegrity(new DataIntegrityViolationException(
            "duplicate key value violates unique constraint \"uq_active_booking\""));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).doesNotContain("uq_active_booking");
    }

    @Test
    void loiKhongMongMuonKhongLoChiTiet() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("bí mật nội bộ"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).doesNotContain("bí mật nội bộ");
    }
}
