package vn.edu.hust.nhom6.gym.common.exception;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;

/**
 * Nơi <b>duy nhất</b> chuyển ngoại lệ thành phản hồi lỗi dạng ProblemDetail (RFC 7807), nội dung tiếng Việt.
 * Chức năng mới không viết {@code @RestControllerAdvice} riêng: service ném ngoại lệ trong
 * {@code common/exception} (hoặc {@code AccessDeniedException} cho 403), lớp này lo phần còn lại.
 *
 * <p>Lỗi dữ liệu đầu vào (400) luôn có {@code errors} dạng {@code {"tênTrường": "thông báo"}} khi biết trường
 * nào sai: body {@code @Valid}, tham số {@code @Positive}/{@code @Max}, tham số sai kiểu, JSON sai cú pháp.
 * Các lỗi chuẩn khác của Spring MVC (sai phương thức, thiếu tham số...) do lớp cha xử lý.
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

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        return problem(HttpStatus.FORBIDDEN, "Không có quyền", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Vi phạm quy tắc nghiệp vụ", ex.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return invalidFields(errors);
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
        return ResponseEntity.badRequest().body(invalidFields(errors));
    }

    /**
     * Lỗi validate tham số đường dẫn/truy vấn, ví dụ {@code @Positive Long id} hay {@code @Max(100) int size}:
     * trả {@code errors} theo tên tham số, cùng dạng với lỗi {@code @Valid} trên body.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.isForReturnValue()) {
            log.error("Giá trị trả về không hợp lệ", ex);
            return ResponseEntity.internalServerError().body(problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Lỗi hệ thống", "Đã có lỗi xảy ra, vui lòng thử lại sau."));
        }
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors fields) {
                fields.getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
            } else {
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error -> errors.putIfAbsent(name, error.getDefaultMessage()));
            }
        });
        return ResponseEntity.badRequest().body(invalidFields(errors));
    }

    /**
     * Body không đọc được: JSON sai cú pháp, sai kiểu, hoặc có trường không được phép (DTO đặt
     * {@code @JsonIgnoreProperties(ignoreUnknown = false)}). {@code errors} trỏ vào trường gây lỗi nếu biết.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = "body";
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof JacksonException json && !json.getPath().isEmpty()) {
                String name = json.getPath().getLast().getPropertyName();
                if (name != null) {
                    field = name;
                }
            }
        }
        return ResponseEntity.badRequest().body(invalidFields(
            Map.of(field, "Trường không được phép hoặc giá trị JSON không hợp lệ.")));
    }

    /** Tham số truy vấn sai kiểu, ví dụ {@code ?page=abc} hay {@code ?type=INVALID} cho tham số enum. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException arg ? arg.getName() : ex.getPropertyName();
        Class<?> type = ex.getRequiredType();
        String message;
        if (type != null && type.isEnum()) {
            List<String> values = Arrays.stream(type.getEnumConstants()).map(String::valueOf).toList();
            message = "Giá trị phải là " + joinVietnamese(values) + ".";
        } else if (type != null && (Number.class.isAssignableFrom(type)
                || type == int.class || type == long.class || type == short.class)) {
            message = "Giá trị phải là số nguyên trong phạm vi cho phép.";
        } else {
            message = "Giá trị không đúng định dạng.";
        }
        return ResponseEntity.badRequest().body(invalidFields(Map.of(name == null ? "body" : name, message)));
    }

    private static ProblemDetail invalidFields(Map<String, String> errors) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ",
            "Vui lòng kiểm tra lại các trường dữ liệu.");
        problem.setProperty("errors", errors);
        return problem;
    }

    /** ["TIME", "SESSION", "PT"] thành "TIME, SESSION hoặc PT". */
    private static String joinVietnamese(List<String> values) {
        if (values.size() <= 1) {
            return String.join("", values);
        }
        return String.join(", ", values.subList(0, values.size() - 1)) + " hoặc " + values.getLast();
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
