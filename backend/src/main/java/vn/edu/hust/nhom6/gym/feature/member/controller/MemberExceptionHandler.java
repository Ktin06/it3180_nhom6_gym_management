package vn.edu.hust.nhom6.gym.feature.member.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.edu.hust.nhom6.gym.common.exception.InvalidRequestException;

/** Lỗi riêng của F2, giữ nguyên xử lý lỗi chung và xác thực F1. */
@RestControllerAdvice(assignableTypes = MemberController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MemberExceptionHandler {
    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail invalidRequest(InvalidRequestException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Dữ liệu không hợp lệ");
        return problem;
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ProblemDetail invalidAuthentication(AuthenticationCredentialsNotFoundException ex) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Chưa xác thực");
        return problem;
    }
}
