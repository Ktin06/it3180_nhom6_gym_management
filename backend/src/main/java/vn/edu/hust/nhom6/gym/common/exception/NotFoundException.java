package vn.edu.hust.nhom6.gym.common.exception;

/** Không tìm thấy dữ liệu (HTTP 404), ví dụ: "Không tìm thấy hội viên có mã HV0001". */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
