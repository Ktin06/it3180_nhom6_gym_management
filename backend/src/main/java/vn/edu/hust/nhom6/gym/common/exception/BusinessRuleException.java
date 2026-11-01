package vn.edu.hust.nhom6.gym.common.exception;

/**
 * Vi phạm quy tắc nghiệp vụ trong bản chốt phạm vi (HTTP 422),
 * ví dụ: "Hội viên chỉ có gói PT nên không check-in được".
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
