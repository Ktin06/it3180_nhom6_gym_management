package vn.edu.hust.nhom6.gym.feature.notification.entity;

import java.util.Arrays;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Bảng đối tượng liên quan lưu trong cột {@code notifications.related_entity}.
 * Dùng để mở đúng màn hình trên cổng hội viên, chống gửi nhắc lặp (BR-F10.2, BR-F10.4),
 * và suy ra chi nhánh của thông báo khi tác vụ gửi email kiểm tra lại giờ mở cửa (BR-F10.3).
 */
public enum NotificationRelatedEntity {
    /** Gói hội viên; chi nhánh lấy từ {@code memberships.branch_id}, rỗng thì lấy {@code members.home_branch_id}. */
    MEMBERSHIPS("memberships"),
    /** Chỗ đặt lớp nhóm; chi nhánh lấy từ {@code class_sessions.branch_id}. */
    CLASS_BOOKINGS("class_bookings"),
    /** Buổi lớp nhóm; chi nhánh lấy từ {@code class_sessions.branch_id}. */
    CLASS_SESSIONS("class_sessions"),
    /** Buổi tập PT; chi nhánh lấy từ {@code pt_sessions.branch_id}. */
    PT_SESSIONS("pt_sessions");

    private final String table;

    NotificationRelatedEntity(String table) {
        this.table = table;
    }

    public String table() {
        return table;
    }

    public static NotificationRelatedEntity fromTable(String table) {
        return Arrays.stream(values())
            .filter(v -> v.table.equals(table))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("related_entity không hợp lệ: " + table));
    }

    @Converter
    public static class JpaConverter implements AttributeConverter<NotificationRelatedEntity, String> {
        @Override
        public String convertToDatabaseColumn(NotificationRelatedEntity attribute) {
            return attribute == null ? null : attribute.table;
        }

        @Override
        public NotificationRelatedEntity convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fromTable(dbData);
        }
    }
}
