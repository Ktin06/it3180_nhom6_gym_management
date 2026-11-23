package vn.edu.hust.nhom6.gym.feature.groupclass.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;
import vn.edu.hust.nhom6.gym.common.exception.BusinessRuleException;

/**
 * Một buổi học cụ thể (bảng {@code class_sessions}), sinh từ lịch lặp bởi tác vụ nền (UC-F8.8). Mỗi lịch có tối đa
 * một buổi cho mỗi {@code occurrence_date} (chỉ mục duy nhất {@code uq_class_sessions_schedule_occurrence}).
 *
 * <p>S2-16.1 chỉ <b>tạo</b> buổi (bằng câu INSERT trong {@code ClassSessionRepository}, để hai lượt sinh chạy cùng lúc
 * không làm hỏng giao dịch) và đọc buổi. Đổi giờ, đổi phòng, hủy buổi, sửa sức chứa (S2-17.1, UC-F8.7 A2–A5) đi qua các
 * hàm nghiệp vụ dưới đây, không có setter. Các hàm chỉ đổi dữ liệu của buổi; điều kiện cần đọc bảng khác (quyền, xung
 * đột, số chỗ chiếm, sức chứa phòng) do {@code ClassSessionChangeService} kiểm trước khi gọi.
 */
@Entity
@Table(name = "class_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassSession extends BaseEntity {

    @Column(name = "class_schedule_id", nullable = false)
    private Long classScheduleId;

    /** Chép từ lịch lúc sinh. */
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /** {@code staff.id}; chép từ lịch lúc sinh, có thể thay riêng cho buổi (BR-F8.8-03). */
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    /** Ngày Việt Nam của lần lặp, giữ nguyên khi đổi giờ (BR-F8.8-02). */
    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    /** Giờ bắt đầu trước lần đổi giờ gần nhất (BR-F8-03); rỗng nếu chưa đổi. */
    @Column(name = "original_start_at")
    private OffsetDateTime originalStartAt;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClassSessionStatus status;

    /** Buổi còn nhận thay đổi theo trạng thái: SCHEDULED hoặc RESCHEDULED (BR-F8.7-02). */
    public boolean isOpen() {
        return status == ClassSessionStatus.SCHEDULED || status == ClassSessionStatus.RESCHEDULED;
    }

    /**
     * Còn sửa được lúc {@code now}: trạng thái SCHEDULED/RESCHEDULED và chưa đến giờ bắt đầu (BR-F8.7-02, AC-F8.7.1-07).
     *
     * @throws BusinessRuleException (422) khi buổi đã hủy, đã kết thúc hoặc đã bắt đầu
     */
    public void requireChangeable(OffsetDateTime now) {
        if (!isOpen()) {
            throw new BusinessRuleException(status == ClassSessionStatus.CANCELLED
                ? "Buổi học đã hủy, không thay đổi được." : "Buổi học đã kết thúc, không thay đổi được.");
        }
        if (!now.isBefore(startAt)) {
            throw new BusinessRuleException("Buổi học đã bắt đầu, không thay đổi được.");
        }
    }

    /**
     * Đổi giờ (BR-F8-03): {@code original_start_at} nhận giờ bắt đầu <b>ngay trước</b> lần đổi này, trạng thái
     * RESCHEDULED; {@code occurrence_date} giữ nguyên (BR-F8.8-02).
     *
     * @throws BusinessRuleException (422) khi kết thúc không sau bắt đầu, hoặc giờ mới trùng đúng giờ đang có
     */
    public void reschedule(OffsetDateTime newStart, OffsetDateTime newEnd) {
        if (!newEnd.isAfter(newStart)) {
            throw new BusinessRuleException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (newStart.isEqual(startAt) && newEnd.isEqual(endAt)) {
            throw new BusinessRuleException("Giờ mới trùng giờ hiện tại của buổi học.");
        }
        originalStartAt = startAt;
        startAt = newStart;
        endAt = newEnd;
        status = ClassSessionStatus.RESCHEDULED;
    }

    /**
     * Đổi phòng (BR-F8.4-03). Sức chứa buổi không được vượt phòng (BR-F8.7-02), nên phòng mới nhỏ hơn thì sức chứa hạ
     * về sức chứa phòng. Nơi gọi đã kiểm tra phòng mới đủ cho số chỗ đang chiếm.
     *
     * @throws BusinessRuleException (422) khi đổi sang đúng phòng đang dùng
     */
    public void changeRoom(Long newRoomId, int roomCapacity) {
        if (newRoomId.equals(roomId)) {
            throw new BusinessRuleException("Buổi học đang dùng phòng này.");
        }
        roomId = newRoomId;
        capacity = Math.min(capacity, roomCapacity);
    }

    /** Hủy buổi (BR-F8.7-05). Các chỗ BOOKED do nơi gọi hủy bằng {@link ClassBooking#cancel}. */
    public void cancel() {
        status = ClassSessionStatus.CANCELLED;
    }

    /**
     * Sửa sức chứa (BR-F8.7-02). Nơi gọi đã kiểm tra giới hạn phòng và số chỗ đang chiếm.
     *
     * @throws BusinessRuleException (422) khi sức chứa không đổi
     */
    public void changeCapacity(int newCapacity) {
        if (newCapacity == capacity) {
            throw new BusinessRuleException("Sức chứa mới trùng sức chứa hiện tại.");
        }
        capacity = newCapacity;
    }
}
