package vn.edu.hust.nhom6.gym.feature.trainer.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.LocalTimeJdbcType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.edu.hust.nhom6.gym.common.entity.BaseEntity;
import vn.edu.hust.nhom6.gym.common.time.VnTime;

/**
 * Khung giờ rảnh lặp hằng tuần của huấn luyện viên (bảng {@code trainer_availabilities}, UC-F9.1).
 *
 * <p>Không có setter công khai: tạo qua {@link #create} và cập nhật qua {@link #update}. Xóa là xóa hẳn dòng
 * (ngoại lệ quy tắc mã số 8 theo BR-F9.15) kèm ghi {@code audit_logs}.
 */
@Entity
@Table(name = "trainer_availabilities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainerAvailability extends BaseEntity {

    /** {@code staff.id} của huấn luyện viên (quy tắc mã 11). */
    @Column(name = "trainer_id", nullable = false)
    private Long trainerId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    /** Thứ ISO: 1 = thứ Hai … 7 = Chủ nhật. */
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    /**
     * Giờ Việt Nam, cột {@code time} không múi giờ. {@link LocalTimeJdbcType} ghi, đọc nguyên giá trị, tránh lệch giờ
     * do {@code hibernate.jdbc.time_zone: UTC} và múi giờ năm 1970 (VD-13, giống {@code ClassSchedule}).
     */
    @JdbcType(LocalTimeJdbcType.class)
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @JdbcType(LocalTimeJdbcType.class)
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    public static TrainerAvailability create(Long trainerId, Long branchId, Integer dayOfWeek,
            LocalTime startTime, LocalTime endTime) {
        TrainerAvailability slot = new TrainerAvailability();
        slot.trainerId = trainerId;
        slot.branchId = branchId;
        slot.dayOfWeek = dayOfWeek;
        slot.startTime = startTime;
        slot.endTime = endTime;
        return slot;
    }

    public void update(Long branchId, Integer dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.branchId = branchId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** Kiểm tra khung này có chồng lên khung {@code (otherDayOfWeek, otherStart, otherEnd)} không (nửa mở). */
    public boolean overlaps(int otherDayOfWeek, LocalTime otherStart, LocalTime otherEnd) {
        return dayOfWeek == otherDayOfWeek && startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }

    /** Kiểm tra buổi {@code [startAt, endAt]} (đổi sang giờ Việt Nam) có chạm vào khung này không (BR-F9.15). */
    public boolean overlapsInVnTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        ZonedDateTime vnStart = startAt.atZoneSameInstant(VnTime.ZONE);
        ZonedDateTime vnEnd = endAt.atZoneSameInstant(VnTime.ZONE);
        LocalDate startDate = vnStart.toLocalDate();
        LocalDate endDate = vnEnd.toLocalDate();
        int startDow = vnStart.getDayOfWeek().getValue();
        LocalTime sessionStart = vnStart.toLocalTime();
        LocalTime sessionEnd = vnEnd.toLocalTime();

        if (endDate.equals(startDate)) {
            return dayOfWeek == startDow && sessionStart.isBefore(endTime) && startTime.isBefore(sessionEnd);
        }
        // Trường hợp buổi vắt qua nửa đêm giờ Việt Nam
        if (dayOfWeek == startDow && sessionStart.isBefore(endTime)) {
            return true;
        }
        int endDow = vnEnd.getDayOfWeek().getValue();
        return dayOfWeek == endDow && sessionEnd.isAfter(LocalTime.MIDNIGHT) && startTime.isBefore(sessionEnd);
    }

    /** Kiểm tra khung này có bao trọn buổi {@code [startAt, endAt]} theo giờ Việt Nam không (BR-F9.2, BR-F9.15). */
    public boolean coversInVnTime(OffsetDateTime startAt, OffsetDateTime endAt) {
        ZonedDateTime vnStart = startAt.atZoneSameInstant(VnTime.ZONE);
        ZonedDateTime vnEnd = endAt.atZoneSameInstant(VnTime.ZONE);
        if (!vnEnd.toLocalDate().equals(vnStart.toLocalDate())) {
            return false;
        }
        int sessionDow = vnStart.getDayOfWeek().getValue();
        return dayOfWeek == sessionDow
            && !vnStart.toLocalTime().isBefore(startTime)
            && !vnEnd.toLocalTime().isAfter(endTime);
    }
}
