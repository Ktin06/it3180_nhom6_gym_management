package vn.edu.hust.nhom6.gym.feature.member.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.validation.method.ParameterErrors;

/** Lỗi riêng của F2, giữ nguyên xử lý lỗi chung và xác thực F1. */
@RestControllerAdvice(assignableTypes = MemberController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MemberExceptionHandler {
    /** @Positive trên đường dẫn bật validation tham số; giữ cùng hợp đồng errors với @Valid DTO. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail invalidMethodArguments(HandlerMethodValidationException ex) {
        if (ex.isForReturnValue()) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã có lỗi xảy ra, vui lòng thử lại sau.");
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
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Vui lòng kiểm tra lại các trường dữ liệu.");
        problem.setTitle("Dữ liệu không hợp lệ");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ProblemDetail invalidAuthentication(AuthenticationCredentialsNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Chưa xác thực");
        return problem;
    }
}
