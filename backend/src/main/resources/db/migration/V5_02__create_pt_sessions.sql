-- Nhóm 5: buổi tập PT 1-1 (F9). Nguồn: erd-v2.dbml
-- trainer_id trỏ tới staff (nhóm 7, tạo sau) nên khóa ngoại thêm ở V8_03.
CREATE TABLE pt_sessions (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    trainer_id        bigint      NOT NULL,
    member_id         bigint      NOT NULL,
    membership_id     bigint      NOT NULL,
    branch_id         bigint      NOT NULL,
    start_at          timestamptz NOT NULL,
    end_at            timestamptz NOT NULL,
    status            varchar(20) NOT NULL DEFAULT 'BOOKED',
    confirmed_at      timestamptz,
    commission_amount integer,
    cancelled_at      timestamptz,
    created_by        bigint,
    note              text,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_pt_sessions_status CHECK (status IN ('BOOKED', 'DONE', 'CANCELLED', 'NO_SHOW')),
    -- commission_amount có giá trị khi và chỉ khi status = DONE (chụp lúc xác nhận buổi)
    CONSTRAINT ck_pt_sessions_commission CHECK ((status = 'DONE') = (commission_amount IS NOT NULL)),
    CONSTRAINT ck_pt_sessions_time CHECK (end_at > start_at)
);
CREATE INDEX ix_pt_sessions_trainer_start ON pt_sessions (trainer_id, start_at);
CREATE INDEX ix_pt_sessions_member_start  ON pt_sessions (member_id, start_at);
CREATE INDEX ix_pt_sessions_membership    ON pt_sessions (membership_id);
