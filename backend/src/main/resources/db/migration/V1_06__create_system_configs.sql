-- Nhóm 1: Tài khoản và hệ thống
-- Bảng system_configs: tham số cấu hình hệ thống (các tham số có dấu * trong bản chốt phạm vi)
-- Lưu ý: khóa ngoại branch_id -> branches(id) thuộc nhóm 2, sẽ được gắn ở cuối dải V8_xx

CREATE TABLE system_configs (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint,
    config_key varchar(100) NOT NULL,
    config_value varchar(255) NOT NULL,
    description varchar(255),
    updated_by bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_system_configs_branch_key UNIQUE NULLS NOT DISTINCT (branch_id, config_key),
    CONSTRAINT fk_system_configs_updated_by FOREIGN KEY (updated_by) REFERENCES users (id)
);

-- Dữ liệu tham chiếu mặc định toàn chuỗi (branch_id = NULL) cần có ở mọi môi trường
INSERT INTO system_configs (branch_id, config_key, config_value, description)
VALUES
    (NULL, 'BOOKING_WINDOW_DAYS', '7', 'Số ngày mở đặt chỗ lớp học nhóm trước giờ học'),
    (NULL, 'PROMOTION_CUTOFF_MIN', '30', 'Số phút trước giờ học ngừng tự động đưa từ danh sách chờ vào lớp'),
    (NULL, 'PROMOTION_GRACE_MIN', '15', 'Số phút ân hạn hủy không phạt sau khi được đưa từ danh sách chờ vào lớp'),
    (NULL, 'RECONFIRM_TTL_MIN', '60', 'Số phút hạn xác nhận lại khi lớp học đổi giờ'),
    (NULL, 'LATE_CANCEL_HOURS', '2', 'Số giờ tối thiểu trước giờ học để hủy chỗ không bị tính hủy muộn'),
    (NULL, 'PENALTY_LIMIT_PER_MONTH', '3', 'Số lần hủy muộn hoặc vắng mặt tối đa trong tháng trước khi khóa đặt lớp'),
    (NULL, 'PENALTY_LOCK_DAYS', '7', 'Số ngày khóa đặt lớp khi vượt ngưỡng hủy muộn hoặc vắng mặt'),
    (NULL, 'PT_MIN_LEAD_HOURS', '12', 'Số giờ đặt trước tối thiểu cho buổi tập PT'),
    (NULL, 'FREEZE_MIN_DAYS', '7', 'Số ngày bảo lưu tối thiểu mỗi lần'),
    (NULL, 'FREEZE_MAX_DAYS', '30', 'Số ngày bảo lưu tối đa mỗi lần'),
    (NULL, 'FREEZE_MAX_TIMES', '2', 'Số lần bảo lưu tối đa cho mỗi gói tập'),
    (NULL, 'TRANSFER_MIN_DAYS', '30', 'Số ngày hạn còn lại tối thiểu của gói để được chuyển nhượng'),
    (NULL, 'TRANSFER_FEE', '200000', 'Phí chuyển nhượng gói tập (VND)'),
    (NULL, 'PENDING_INVOICE_TTL_DAYS', '3', 'Số ngày tối đa giữ hóa đơn chờ từ cổng hội viên trước khi tự động hủy'),
    (NULL, 'SESSION_WARNING_THRESHOLD', '2', 'Số buổi còn lại tối đa của gói SESSION để kích hoạt cảnh báo sắp hết buổi')
