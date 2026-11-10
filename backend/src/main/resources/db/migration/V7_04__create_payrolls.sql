-- Nhóm 7: bảng lương. Nguồn: erd-v2.dbml
CREATE TABLE payrolls (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    staff_id          bigint      NOT NULL,
    branch_id         bigint      NOT NULL,            -- chép từ staff.branch_id lúc tính lương
    month             varchar(7)  NOT NULL,            -- YYYY-MM
    base_amount       integer         NOT NULL,
    commission_amount integer         NOT NULL DEFAULT 0,  -- tổng pt_sessions.commission_amount của buổi DONE trong tháng
    deduction         integer         NOT NULL DEFAULT 0,
    total             integer         NOT NULL,
    status            varchar(20) NOT NULL DEFAULT 'DRAFT',
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_payrolls_staff  FOREIGN KEY (staff_id)  REFERENCES staff (id),
    CONSTRAINT ck_payrolls_status CHECK (status IN ('DRAFT', 'CONFIRMED', 'PAID')),
    CONSTRAINT ck_payrolls_amounts CHECK (base_amount >= 0 AND commission_amount >= 0 AND deduction >= 0),
    CONSTRAINT ck_payrolls_total CHECK (total = base_amount + commission_amount - deduction),
    CONSTRAINT ck_payrolls_month CHECK (month ~ '^[0-9]{4}-(0[1-9]|1[0-2])$')
);
CREATE UNIQUE INDEX uq_payrolls_staff_month ON payrolls (staff_id, month);
