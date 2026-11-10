-- Nhóm 7: ca làm việc. Nguồn: erd-v2.dbml
CREATE TABLE shifts (
    id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    staff_id   bigint      NOT NULL,
    branch_id  bigint      NOT NULL,
    start_at   timestamptz NOT NULL,
    end_at     timestamptz NOT NULL,
    created_by bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_shifts_staff      FOREIGN KEY (staff_id)   REFERENCES staff (id),
    CONSTRAINT ck_shifts_time CHECK (end_at > start_at)
);
CREATE INDEX ix_shifts_staff_start ON shifts (staff_id, start_at);
