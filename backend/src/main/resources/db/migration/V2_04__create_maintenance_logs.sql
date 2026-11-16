-- Nhóm 2: Chi nhánh và cơ sở vật chất
-- Bảng maintenance_logs: nhật ký báo hỏng và bảo trì thiết bị (F11)
-- Lưu ý: khóa ngoại reported_by -> users(id) thuộc nhóm 1, sẽ được gắn ở cuối dải V8_xx

CREATE TABLE maintenance_logs (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    equipment_id bigint NOT NULL,
    reported_by bigint NOT NULL,
    issue text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'REPORTED',
    scheduled_date date,
    completed_date date,
    cost integer,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_maintenance_logs_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id),
    CONSTRAINT ck_maintenance_logs_status CHECK (status IN ('REPORTED', 'SCHEDULED', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_maintenance_logs_cost CHECK (cost IS NULL OR cost >= 0)
);

CREATE INDEX ix_maintenance_logs_equipment_id ON maintenance_logs (equipment_id);
