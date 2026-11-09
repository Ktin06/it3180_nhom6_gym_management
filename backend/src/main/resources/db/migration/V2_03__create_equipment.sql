-- Nhóm 2: Chi nhánh và cơ sở vật chất
-- Bảng equipment: thiết bị, máy tập trong chi nhánh (F11)

CREATE TABLE equipment (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint NOT NULL,
    room_id bigint,
    code varchar(50),
    name varchar(255) NOT NULL,
    type varchar(50),
    status varchar(20) NOT NULL DEFAULT 'GOOD',
    purchased_at date,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_equipment_code UNIQUE (code),
    CONSTRAINT fk_equipment_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT fk_equipment_room FOREIGN KEY (room_id) REFERENCES rooms (id),
    CONSTRAINT ck_equipment_status CHECK (status IN ('GOOD', 'BROKEN', 'MAINTENANCE', 'RETIRED'))
);
