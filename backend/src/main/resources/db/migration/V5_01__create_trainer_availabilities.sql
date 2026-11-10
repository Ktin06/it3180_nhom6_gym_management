-- Nhóm 5: lịch rảnh của huấn luyện viên (F9). Nguồn: erd-v2.dbml
-- trainer_id trỏ tới staff (nhóm 7, tạo sau) nên khóa ngoại thêm ở V8_03.
CREATE TABLE trainer_availabilities (
    id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trainer_id  bigint      NOT NULL,
    branch_id   bigint      NOT NULL,
    day_of_week integer         NOT NULL,
    start_time  time        NOT NULL,
    end_time    time        NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_trainer_availabilities_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_trainer_availabilities_time CHECK (end_time > start_time)
);
CREATE INDEX ix_trainer_availabilities_trainer_day ON trainer_availabilities (trainer_id, day_of_week);
