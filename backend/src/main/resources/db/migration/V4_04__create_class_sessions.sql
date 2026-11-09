-- Nhóm 4: Check-in và lớp học
-- Bảng class_sessions: từng buổi học cụ thể sinh ra từ lịch lặp hằng tuần (F8)
-- Lưu ý: khóa ngoại nội bộ class_schedule_id -> class_schedules(id).
-- Các khóa ngoại liên nhóm (branch_id -> branches, trainer_id -> staff, room_id -> rooms)
-- được gắn ở cuối dải V8_xx

CREATE TABLE class_sessions (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    class_schedule_id bigint NOT NULL,
    branch_id bigint NOT NULL,
    trainer_id bigint NOT NULL,
    room_id bigint NOT NULL,
    occurrence_date date NOT NULL,
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    original_start_at timestamptz,
    capacity integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_class_sessions_schedule FOREIGN KEY (class_schedule_id) REFERENCES class_schedules (id),
    CONSTRAINT ck_class_sessions_time CHECK (end_at > start_at),
    CONSTRAINT ck_class_sessions_capacity CHECK (capacity > 0),
    CONSTRAINT ck_class_sessions_status CHECK (status IN ('SCHEDULED', 'RESCHEDULED', 'CANCELLED', 'COMPLETED'))
);

CREATE UNIQUE INDEX uq_class_sessions_schedule_occurrence ON class_sessions (class_schedule_id, occurrence_date);
CREATE INDEX ix_class_sessions_branch_start ON class_sessions (branch_id, start_at);
CREATE INDEX ix_class_sessions_trainer_start ON class_sessions (trainer_id, start_at);
