-- Nhóm 3: Hội viên và gói tập
-- Bảng packages: danh mục gói tập đang bán (F3)
-- Lưu ý: khóa ngoại liên nhóm (branch_id -> branches) được gắn ở cuối dải V8_xx

CREATE TABLE packages (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint,
    code varchar(30) NOT NULL,
    name varchar(255) NOT NULL,
    type varchar(20) NOT NULL,
    duration_days integer NOT NULL,
    session_count integer,
    price integer NOT NULL,
    description text,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_packages_code UNIQUE (code),
    CONSTRAINT ck_packages_type CHECK (type IN ('TIME', 'SESSION', 'PT')),
    CONSTRAINT ck_packages_duration_days CHECK (duration_days > 0),
    CONSTRAINT ck_packages_price CHECK (price >= 0),
    CONSTRAINT ck_packages_session_count CHECK (
        (type = 'TIME' AND session_count IS NULL) OR
        (type IN ('SESSION', 'PT') AND session_count IS NOT NULL)
    ),
    CONSTRAINT ck_packages_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

