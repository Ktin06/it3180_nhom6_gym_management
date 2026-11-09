-- Nhóm 8: Gamification (F19)
-- Bảng badges: Danh mục huy hiệu và điều kiện đạt

CREATE TABLE badges (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(50) NOT NULL UNIQUE,
    name varchar(100) NOT NULL,
    description text,
    icon_url varchar(500),
    rule jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
