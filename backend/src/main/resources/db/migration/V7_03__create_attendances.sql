-- Nhóm 7: chấm công. Nguồn: erd-v2.dbml
CREATE TABLE attendances (
  id           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  staff_id     bigint      NOT NULL,
  shift_id     bigint      NOT NULL,
  check_in_at  timestamptz,
  check_out_at timestamptz,
  status       varchar(20) NOT NULL,
  created_at   timestamptz NOT NULL DEFAULT now(),
  updated_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_attendances_staff FOREIGN KEY (staff_id) REFERENCES staff (id),
  CONSTRAINT fk_attendances_shift FOREIGN KEY (shift_id) REFERENCES shifts (id),
  CONSTRAINT ck_attendances_status CHECK (status IN ('ON_TIME', 'LATE', 'EARLY_LEAVE', 'ABSENT'))
);
-- mỗi ca một bản ghi chấm công
CREATE UNIQUE INDEX uq_attendances_shift ON attendances (shift_id);
