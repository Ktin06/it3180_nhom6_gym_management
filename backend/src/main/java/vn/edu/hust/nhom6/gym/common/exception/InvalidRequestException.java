package vn.edu.hust.nhom6.gym.common.exception;

/** Dữ liệu đầu vào sai định dạng, trả HTTP 400. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
