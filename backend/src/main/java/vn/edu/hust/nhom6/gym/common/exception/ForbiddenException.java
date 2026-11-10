package vn.edu.hust.nhom6.gym.common.exception;

/** Ngoại lệ khi tài khoản bị khóa, ngừng hoạt động hoặc bị từ chối truy cập (403 Forbidden). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
