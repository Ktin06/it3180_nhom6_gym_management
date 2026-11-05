-- Nhóm 5: buổi tập PT 1-1 (F9). Nguồn: erd-v2.dbml
-- trainer_id trỏ tới staff (nhóm 7, tạo sau) nên khóa ngoại thêm ở V7_05.
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
  commission_amount int,
  cancelled_at      timestamptz,
  created_by        bigint,
  note              text,
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_pt_sessions_member     FOREIGN KEY (member_id)     REFERENCES members (id),
  CONSTRAINT fk_pt_sessions_membership FOREIGN KEY (membership_id) REFERENCES memberships (id),
  CONSTRAINT fk_pt_sessions_branch     FOREIGN KEY (branch_id)     REFERENCES branches (id),
  CONSTRAINT fk_pt_sessions_created_by FOREIGN KEY (created_by)    REFERENCES users (id),
  CONSTRAINT ck_pt_sessions_status CHECK (status IN ('BOOKED', 'DONE', 'CANCELLED', 'NO_SHOW')),
  -- commission_amount có giá trị khi và chỉ khi status = DONE (chụp lúc xác nhận buổi)
  CONSTRAINT ck_pt_sessions_commission CHECK ((status = 'DONE') = (commission_amount IS NOT NULL))
);
CREATE INDEX ix_pt_sessions_trainer_start ON pt_sessions (trainer_id, start_at);
CREATE INDEX ix_pt_sessions_member_start  ON pt_sessions (member_id, start_at);
CREATE INDEX ix_pt_sessions_membership    ON pt_sessions (membership_id);
