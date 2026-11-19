-- Bỏ danh sách chờ và xác nhận lại khi đổi giờ của lớp học nhóm (ADR 0014, thay ADR 0010).
-- Lớp nhóm còn chỗ thì đặt, đầy là hết chỗ; đổi giờ thì giữ chỗ.
-- Chưa có API F8 trên main và dữ liệu mẫu không tạo dòng class_bookings, nên các cột bị bỏ đang trống.
-- Vẫn chặn lại nếu có dữ liệu, để không âm thầm làm mất chỗ đặt.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM class_bookings
               WHERE status IN ('WAITLIST', 'PENDING_RECONFIRM') OR cancel_reason = 'NOT_RECONFIRMED') THEN
        RAISE EXCEPTION 'Còn chỗ đặt ở trạng thái danh sách chờ hoặc chờ xác nhận lại; xử lý trước khi chạy migration V9_03';
    END IF;
END $$;

ALTER TABLE class_bookings DROP CONSTRAINT ck_class_bookings_status;
ALTER TABLE class_bookings ADD CONSTRAINT ck_class_bookings_status
    CHECK (status IN ('BOOKED', 'CANCELLED', 'ATTENDED', 'NO_SHOW'));

ALTER TABLE class_bookings DROP CONSTRAINT ck_class_bookings_cancel_reason;
ALTER TABLE class_bookings ADD CONSTRAINT ck_class_bookings_cancel_reason
    CHECK (cancel_reason IS NULL OR cancel_reason IN ('BY_MEMBER', 'BY_STAFF', 'SESSION_CANCELLED'));

DROP INDEX ix_class_bookings_waitlist;

ALTER TABLE class_bookings
    DROP COLUMN waitlisted_at,
    DROP COLUMN promoted_at,
    DROP COLUMN grace_until,
    DROP COLUMN reconfirm_expires_at;

-- Tham số cấu hình (không phải dữ liệu nghiệp vụ): bỏ hai tham số không còn dùng,
-- đổi tên mốc đóng đặt chỗ vì không còn "đưa vào lớp" (promotion).
DELETE FROM system_configs WHERE config_key IN ('PROMOTION_GRACE_MIN', 'RECONFIRM_TTL_MIN');
UPDATE system_configs
   SET config_key = 'BOOKING_CUTOFF_MIN',
       description = 'Số phút trước giờ học đóng đặt chỗ lớp học nhóm'
 WHERE config_key = 'PROMOTION_CUTOFF_MIN';
