-- Nhóm 7: bảng lương. Nguồn: erd-v2.dbml
CREATE TABLE payrolls (
  id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  staff_id          bigint      NOT NULL,
  branch_id         bigint      NOT NULL,            -- chép từ staff.branch_id lúc tính lương
  month             varchar(7)  NOT NULL,            -- YYYY-MM
  base_amount       int         NOT NULL,
  commission_amount int         NOT NULL DEFAULT 0,  -- tổng pt_sessions.commission_amount của buổi DONE trong tháng
  deduction         int         NOT NULL DEFAULT 0,
  total             int         NOT NULL,
  status            varchar(20) NOT NULL DEFAULT 'DRAFT',
  created_at        timestamptz NOT NULL DEFAULT now(),
  updated_at        timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fk_payrolls_staff  FOREIGN KEY (staff_id)  REFERENCES staff (id),
  CONSTRAINT fk_payrolls_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
  CONSTRAINT ck_payrolls_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'PAID'))
);
CREATE UNIQUE INDEX uq_payrolls_staff_month ON payrolls (staff_id, month);
