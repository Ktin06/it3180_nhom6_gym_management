-- Nhóm 7: nhân viên. Nguồn: erd-v2.dbml
CREATE TABLE staff (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         bigint       NOT NULL,
    branch_id       bigint       NOT NULL,
    staff_code      varchar(20)  NOT NULL,
    full_name       varchar(255) NOT NULL,
    phone           varchar(20),
    base_salary     integer          NOT NULL DEFAULT 0,
    hourly_rate     integer,
    commission_rate decimal(5,4),
    specialties     varchar(255),
    hired_at        date,
    status          varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uq_staff_user_id    UNIQUE (user_id),
    CONSTRAINT uq_staff_staff_code UNIQUE (staff_code),
    CONSTRAINT ck_staff_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_staff_commission_rate CHECK (commission_rate IS NULL OR (commission_rate >= 0 AND commission_rate <= 1)),
    CONSTRAINT ck_staff_salary CHECK (base_salary >= 0 AND (hourly_rate IS NULL OR hourly_rate >= 0)),
    -- cùng định dạng chuẩn với members.phone: 10 chữ số bắt đầu bằng 0
    CONSTRAINT ck_staff_phone  CHECK (phone ~ '^0[0-9]{9}$')
);
