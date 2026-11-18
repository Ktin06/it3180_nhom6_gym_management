package vn.edu.hust.nhom6.gym.feature.groupclass.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.core.JacksonException;
import org.springframework.validation.method.ParameterErrors;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;

/** Giữ lỗi tham số F8 bằng tiếng Việt và errors theo trường, không đổi handler các chức năng khác. */
@RestControllerAdvice(assignableTypes = ClassController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ClassExceptionHandler {
    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public ProblemDetail lockConflict(org.springframework.dao.PessimisticLockingFailureException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "Dữ liệu đang được cập nhật. Vui lòng tải lại và thử lại.");
        problem.setTitle("Xung đột dữ liệu");
        return problem;
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail invalidRequest(InvalidRequestException ex) {
        return invalid(Map.of("body", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail invalidBody(HttpMessageNotReadableException ex) {
        String field = "body";
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof JacksonException json && !json.getPath().isEmpty()) {
                String name = json.getPath().getLast().getPropertyName();
                if (name != null) { field = name; }
            }
        }
        return invalid(Map.of(field, "Trường không được phép hoặc giá trị JSON không hợp lệ."));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail invalidArguments(HandlerMethodValidationException ex) {
        if (ex.isForReturnValue()) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã có lỗi xảy ra, vui lòng thử lại sau.");
        }
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors fields) {
                fields.getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
            } else {
                result.getResolvableErrors().forEach(error -> errors.putIfAbsent(
                    result.getMethodParameter().getParameterName(), error.getDefaultMessage()));
            }
        });
        return invalid(errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail invalidType(MethodArgumentTypeMismatchException ex) {
        String message = switch (ex.getName()) {
            default -> "Giá trị phải là số nguyên trong phạm vi cho phép.";
        };
        return invalid(Map.of(ex.getName(), message));
    }

    private ProblemDetail invalid(Map<String, String> errors) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Vui lòng kiểm tra lại các trường dữ liệu.");
        problem.setTitle("Dữ liệu không hợp lệ");
        problem.setProperty("errors", errors);
        return problem;
    }
}
