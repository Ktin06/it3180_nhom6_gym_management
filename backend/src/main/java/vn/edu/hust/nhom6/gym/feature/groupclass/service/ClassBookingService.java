package vn.edu.hust.nhom6.gym.feature.groupclass.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import vn.edu.hust.nhom6.gym.feature.groupclass.entity.ClassBooking;
import vn.edu.hust.nhom6.gym.feature.groupclass.repository.ClassBookingRepository;

/**
 * Quản lý chỗ đặt lớp học nhóm (F8) và phục vụ check-in tại quầy (F4).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassBookingService {
    private final ClassBookingRepository bookings;

    /**
     * Ghi {@code arrived_at = now} cho các chỗ BOOKED của hội viên ở buổi thuộc chi nhánh {@code branchId}, bắt đầu trong
     * {@code [from, to]} (S2-11.1, UC-F4.3). Khung giờ do F4 truyền vào; F8 chỉ ghi cột này, không đổi {@code status}.
     * Chỗ đã NO_SHOW, ATTENDED, CANCELLED, hoặc đã có {@code arrived_at} không bị đụng (kể cả {@code updated_at}).
     *
     * <p>Khóa từng chỗ ({@code FOR UPDATE}) theo id tăng dần rồi kiểm lại. Nơi gọi <b>phải đã khóa</b> {@code members}
     * (và {@code memberships} nếu có) trước, đúng thứ tự members → memberships → class_bookings, khớp hủy chỗ và điểm
     * danh (members → … → class_bookings). Gọi trong giao dịch của nơi gọi.
     *
     * @return id các chỗ vừa ghi, tăng dần; rỗng nếu không có
     */
    @Transactional
    public List<Long> markArrived(Long memberId, Long branchId, OffsetDateTime from, OffsetDateTime to,
            OffsetDateTime now) {
        List<Long> marked = new ArrayList<>();
        for (Long id : bookings.findArrivableIds(memberId, branchId, from, to)) {
            ClassBooking booking = bookings.findByIdForUpdate(id).orElse(null);
            // Trong lúc chờ khóa, huấn luyện viên có thể đã điểm danh hoặc chỗ đã bị hủy: bỏ qua, không đổi gì.
            if (booking != null && booking.markArrived(now)) {
                bookings.saveAndFlush(booking);
                marked.add(id);
            }
        }
        return marked;
    }
}
