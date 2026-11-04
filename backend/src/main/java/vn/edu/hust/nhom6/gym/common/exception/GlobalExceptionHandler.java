package vn.edu.hust.nhom6.gym.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Chuyển ngoại lệ thành phản hồi lỗi dạng ProblemDetail (RFC 7807), nội dung tiếng Việt.
 *
 * <p>Các lỗi chuẩn của Spring MVC (sai phương thức, thiếu tham số...) do lớp cha xử lý.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Không tìm thấy", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Xung đột dữ liệu", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Chưa xác thực", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Vi phạm quy tắc nghiệp vụ", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ",
            "Vui lòng kiểm tra lại các trường dữ liệu.");
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Hai người cùng sửa một dòng có cột version (@Version), ví dụ cùng xác nhận thanh toán một hóa đơn:
     * người lưu sau bị từ chối (409) thay vì rơi vào lỗi 500.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "Xung đột dữ liệu",
            "Dữ liệu vừa được người khác cập nhật. Vui lòng tải lại và thử lại.");
    }

    /**
     * Lưới an toàn khi vi phạm ràng buộc của cơ sở dữ liệu (chỉ mục duy nhất, CHECK, khóa ngoại), ví dụ hai
     * yêu cầu đặt cùng một chỗ lớp lọt qua kiểm tra trong service. Service vẫn phải tự kiểm tra trước và ném
     * {@link ConflictException} với thông điệp cụ thể; tới đây là lỗi chưa được kiểm tra nên ghi log.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc cơ sở dữ liệu: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Xung đột dữ liệu",
            "Dữ liệu bị trùng hoặc không còn hợp lệ. Vui lòng tải lại và thử lại.");
    }

    /** Lỗi phân quyền từ @PreAuthorize: trả 403 thay vì rơi vào lỗi 500 bên dưới. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Không có quyền", "Bạn không có quyền thực hiện thao tác này.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Lỗi không mong muốn", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống",
            "Đã có lỗi xảy ra, vui lòng thử lại sau.");
    }

    /** Lỗi Bean Validation trên @RequestBody (S5): trả danh sách lỗi theo từng trường. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ",
            "Vui lòng kiểm tra lại các trường dữ liệu.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
