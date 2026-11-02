-- Nhóm 2: Chi nhánh và cơ sở vật chất
-- Bảng branches: chi nhánh phòng tập (từ v1 đến v3 chạy với một chi nhánh mặc định CN01)

CREATE TABLE branches (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(20) NOT NULL,
    name varchar(255) NOT NULL,
    address varchar(255),
    phone varchar(20),
    open_time time NOT NULL,
    close_time time NOT NULL,
    capacity integer,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_branches_code UNIQUE (code),
    CONSTRAINT ck_branches_open_close_time CHECK (close_time > open_time),
    CONSTRAINT ck_branches_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT ck_branches_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Chi nhánh mặc định cần có ở mọi môi trường (v1 - v3)
INSERT INTO branches (code, name, address, phone, open_time, close_time, capacity, status)
VALUES ('CN01', 'Chi nhánh Bách Khoa', 'Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội', '0901234567', '05:30:00', '22:00:00', 200, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;
