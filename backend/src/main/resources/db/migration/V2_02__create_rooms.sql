-- Nhóm 2: Chi nhánh và cơ sở vật chất
-- Bảng rooms: phòng tập trong chi nhánh (F8)

CREATE TABLE rooms (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint NOT NULL,
    name varchar(100) NOT NULL,
    type varchar(50),
    capacity integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_rooms_branch_name UNIQUE (branch_id, name),
    CONSTRAINT fk_rooms_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT ck_rooms_capacity CHECK (capacity > 0),
    CONSTRAINT ck_rooms_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
