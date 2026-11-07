-- Nhóm 4: Check-in và lớp học
-- Bảng class_schedules: khung lịch lớp học lặp lại hằng tuần theo chi nhánh (F8)
-- Lưu ý: khóa ngoại nội bộ class_type_id -> class_types(id).
-- Các khóa ngoại liên nhóm (branch_id -> branches, trainer_id -> staff, room_id -> rooms)
-- được gắn ở cuối dải V8_xx

CREATE TABLE class_schedules (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint NOT NULL,
    class_type_id bigint NOT NULL,
    trainer_id bigint NOT NULL,
    room_id bigint NOT NULL,
    day_of_week integer NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    capacity integer NOT NULL,
    valid_from date NOT NULL,
    valid_to date,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_class_schedules_class_type FOREIGN KEY (class_type_id) REFERENCES class_types (id),
    CONSTRAINT ck_class_schedules_day_of_week CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_class_schedules_time CHECK (end_time > start_time),
    CONSTRAINT ck_class_schedules_capacity CHECK (capacity > 0),
    CONSTRAINT ck_class_schedules_valid_dates CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT ck_class_schedules_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX ix_class_schedules_branch_status ON class_schedules (branch_id, status);
