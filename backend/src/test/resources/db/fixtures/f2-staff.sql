-- Fixture riêng cho kiểm thử F2 khi migration nhóm 7 chưa gộp.
-- Không nằm trong Flyway production, không thay migration do người phụ trách nhóm 7 viết.
CREATE TABLE IF NOT EXISTS staff (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint UNIQUE NOT NULL REFERENCES users(id),
    branch_id bigint NOT NULL REFERENCES branches(id),
    staff_code varchar(20) UNIQUE NOT NULL,
    full_name varchar(255) NOT NULL,
    phone varchar(20),
    base_salary integer NOT NULL DEFAULT 0,
    hourly_rate integer,
    commission_rate decimal(5,4),
    specialties varchar(255),
    hired_at date,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
