-- Nhóm 3: Hội viên và gói tập
-- Bảng membership_freezes: yêu cầu bảo lưu gói tập và kết quả duyệt (F15)
-- Lưu ý: các khóa ngoại liên nhóm (requested_by -> users, approved_by -> users)
-- được gắn ở cuối dải V8_xx

CREATE TABLE membership_freezes (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    membership_id bigint NOT NULL,
    requested_by bigint NOT NULL,
    approved_by bigint,
    from_date date NOT NULL,
    to_date date NOT NULL,
    reason text,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    decided_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_membership_freezes_membership FOREIGN KEY (membership_id) REFERENCES memberships (id),
    CONSTRAINT ck_membership_freezes_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX ix_membership_freezes_membership_id ON membership_freezes (membership_id);

