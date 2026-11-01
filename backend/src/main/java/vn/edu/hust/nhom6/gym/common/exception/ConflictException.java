package vn.edu.hust.nhom6.gym.common.exception;

/** Xung đột với dữ liệu đã có (HTTP 409), ví dụ: "Số điện thoại đã thuộc một hồ sơ hội viên". */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
