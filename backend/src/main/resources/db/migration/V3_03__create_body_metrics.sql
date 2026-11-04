-- Nhóm 3: Hội viên và gói tập
-- Bảng body_metrics: chỉ số cơ thể theo từng lần đo (F2, F9)
-- Lưu ý: khóa ngoại liên nhóm (recorded_by -> users) được gắn ở cuối dải V8_xx

CREATE TABLE body_metrics (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id bigint NOT NULL,
    recorded_by bigint,
    measured_at date NOT NULL,
    weight numeric(5, 2),
    height numeric(5, 2),
    body_fat numeric(4, 2),
    muscle_mass numeric(5, 2),
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_body_metrics_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT ck_body_metrics_weight CHECK (weight IS NULL OR weight > 0),
    CONSTRAINT ck_body_metrics_height CHECK (height IS NULL OR height > 0),
    CONSTRAINT ck_body_metrics_body_fat CHECK (body_fat IS NULL OR (body_fat >= 0 AND body_fat <= 100)),
    CONSTRAINT ck_body_metrics_muscle_mass CHECK (muscle_mass IS NULL OR muscle_mass > 0)
);

CREATE INDEX ix_body_metrics_member_measured ON body_metrics (member_id, measured_at DESC);

