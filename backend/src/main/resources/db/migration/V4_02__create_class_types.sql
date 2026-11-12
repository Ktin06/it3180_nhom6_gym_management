-- Nhóm 4: Check-in và lớp học
-- Bảng class_types: danh mục loại lớp học nhóm (Yoga, Zumba, Boxing...) (F8)

CREATE TABLE class_types (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name varchar(100) NOT NULL,
    description text,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_class_types_name UNIQUE (name),
    CONSTRAINT ck_class_types_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
