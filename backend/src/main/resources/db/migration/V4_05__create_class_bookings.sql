-- Nhóm 4: Check-in và lớp học
-- Bảng class_bookings: đặt chỗ và danh sách chờ lớp học nhóm (F8, F10)
-- Lưu ý: khóa ngoại nội bộ class_session_id -> class_sessions(id).
-- Các khóa ngoại liên nhóm (member_id -> members, created_by -> users,
-- attendance_marked_by -> users) được gắn ở cuối dải V8_xx

CREATE TABLE class_bookings (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_session_id bigint NOT NULL,
    member_id bigint NOT NULL,
    created_by bigint,
    status varchar(20) NOT NULL,
    waitlisted_at timestamptz,
    booked_at timestamptz,
    promoted_at timestamptz,
    grace_until timestamptz,
    reconfirm_expires_at timestamptz,
    arrived_at timestamptz,
    cancelled_at timestamptz,
    cancel_reason varchar(30),
    is_late_cancel boolean NOT NULL DEFAULT false,
    attendance_marked_by bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_class_bookings_session FOREIGN KEY (class_session_id) REFERENCES class_sessions (id),
    CONSTRAINT ck_class_bookings_status CHECK (status IN ('BOOKED', 'WAITLIST', 'PENDING_RECONFIRM', 'CANCELLED', 'ATTENDED', 'NO_SHOW')),
    CONSTRAINT ck_class_bookings_cancel_reason CHECK (cancel_reason IS NULL OR cancel_reason IN ('BY_MEMBER', 'BY_STAFF', 'NOT_RECONFIRMED', 'SESSION_CANCELLED')),
    CONSTRAINT ck_class_bookings_cancelled CHECK (
        (status = 'CANCELLED') = (cancelled_at IS NOT NULL AND cancel_reason IS NOT NULL)
        AND (is_late_cancel = false OR status = 'CANCELLED')
    )
);

CREATE UNIQUE INDEX uq_active_booking ON class_bookings (class_session_id, member_id) WHERE status <> 'CANCELLED';
CREATE INDEX ix_class_bookings_waitlist ON class_bookings (class_session_id, status, waitlisted_at);
CREATE INDEX ix_class_bookings_member_status ON class_bookings (member_id, status);
