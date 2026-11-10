package vn.edu.hust.nhom6.gym.common.exception;

/** Ngoại lệ khi yêu cầu xác thực không thành công (401 Unauthorized), trả ProblemDetail tiếng Việt. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
