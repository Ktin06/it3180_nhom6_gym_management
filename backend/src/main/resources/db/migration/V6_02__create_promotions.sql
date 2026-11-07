-- Nhóm 6: Bán hàng và thanh toán. Tạo bảng promotions.
-- Khóa ngoại liên nhóm được thêm ở V8_03.

CREATE TABLE promotions (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint,
    code varchar(50) NOT NULL UNIQUE,
    type varchar(20) NOT NULL,
    value integer NOT NULL,
    max_discount integer,
    min_amount integer,
    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    usage_limit integer,
    used_count integer NOT NULL DEFAULT 0,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_promotions_type CHECK (type IN ('PERCENT', 'FIXED')),
    CONSTRAINT ck_promotions_value CHECK (
        (type = 'PERCENT' AND value BETWEEN 1 AND 100)
        OR (type = 'FIXED' AND value >= 0)
    ),
    CONSTRAINT ck_promotions_max_discount CHECK (max_discount IS NULL OR max_discount >= 0),
    CONSTRAINT ck_promotions_percent_max_discount CHECK (type = 'PERCENT' OR max_discount IS NULL),
    CONSTRAINT ck_promotions_min_amount CHECK (min_amount IS NULL OR min_amount >= 0),
    CONSTRAINT ck_promotions_time_range CHECK (end_at > start_at),
    CONSTRAINT ck_promotions_usage_limit CHECK (usage_limit IS NULL OR usage_limit > 0),
    CONSTRAINT ck_promotions_used_count CHECK (
        used_count >= 0 AND (usage_limit IS NULL OR used_count <= usage_limit)
    ),
    CONSTRAINT ck_promotions_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX ix_promotions_branch_id ON promotions(branch_id);
