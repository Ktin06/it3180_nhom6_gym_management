-- Nhóm 3: Hội viên và gói tập
-- Bảng members: hồ sơ hội viên (F2, F4, F7)
-- Lưu ý: các khóa ngoại liên nhóm (user_id -> users, home_branch_id -> branches,
-- created_by -> users) được gắn ở cuối dải V8_xx

CREATE SEQUENCE member_code_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE members (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint,
    home_branch_id bigint NOT NULL,
    member_code varchar(20) NOT NULL,
    full_name varchar(255) NOT NULL,
    gender varchar(10),
    dob date,
    phone varchar(20) NOT NULL,
    avatar_url varchar(500),
    face_consent_at timestamptz,
    booking_locked_until timestamptz,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at timestamptz,
    created_by bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_members_user_id UNIQUE (user_id),
    CONSTRAINT uq_members_member_code UNIQUE (member_code),
    CONSTRAINT uq_members_phone UNIQUE (phone),
    CONSTRAINT ck_members_member_code CHECK (member_code ~ '^HV[0-9]{6}$'),
    CONSTRAINT ck_members_phone CHECK (phone ~ '^0[0-9]{9}$'),
    CONSTRAINT ck_members_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX ix_members_full_name ON members (full_name);
CREATE INDEX ix_members_home_branch ON members (home_branch_id);

