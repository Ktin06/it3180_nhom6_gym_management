-- Nhóm 4: Check-in và lớp học
-- Bảng checkins: mỗi lượt hội viên vào phòng tập (F4, F6, F20)
-- Lưu ý: các khóa ngoại liên nhóm (member_id -> members, membership_id -> memberships,
-- branch_id -> branches, created_by -> users) được gắn ở cuối dải V8_xx

CREATE TABLE checkins (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id bigint NOT NULL,
    membership_id bigint NOT NULL,
    branch_id bigint NOT NULL,
    created_by bigint NOT NULL,
    method varchar(20) NOT NULL DEFAULT 'MANUAL',
    match_score numeric(5, 4),
    checkin_date date NOT NULL,
    session_deducted boolean NOT NULL DEFAULT false,
    checked_in_at timestamptz NOT NULL DEFAULT now(),
    checked_out_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_checkins_method CHECK (method IN ('MANUAL', 'FACE')),
    CONSTRAINT ck_checkins_match_score CHECK (match_score IS NULL OR (match_score >= 0 AND match_score <= 1)),
    CONSTRAINT ck_checkins_checkout CHECK (checked_out_at IS NULL OR checked_out_at >= checked_in_at)
);

CREATE INDEX ix_checkins_branch_checked_in ON checkins (branch_id, checked_in_at);
CREATE INDEX ix_checkins_member_checked_in ON checkins (member_id, checked_in_at);
CREATE UNIQUE INDEX uq_one_deduction_per_day ON checkins (membership_id, checkin_date) WHERE session_deducted = true;
