-- Nhóm 3: Hội viên và gói tập
-- Bảng memberships: gói tập một hội viên đã mua (F3, F4, F7)
-- Lưu ý: các khóa ngoại liên nhóm (branch_id -> branches, sold_branch_id -> branches,
-- invoice_item_id -> invoice_items) được gắn ở cuối dải V8_xx

CREATE TABLE memberships (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id bigint NOT NULL,
    package_id bigint NOT NULL,
    package_type varchar(20) NOT NULL,
    branch_id bigint,
    sold_branch_id bigint NOT NULL,
    invoice_item_id bigint,
    start_date date NOT NULL,
    end_date date NOT NULL,
    total_sessions integer,
    remaining_sessions integer,
    freeze_count integer NOT NULL DEFAULT 0,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_memberships_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_memberships_package FOREIGN KEY (package_id) REFERENCES packages (id),
    CONSTRAINT ck_memberships_package_type CHECK (package_type IN ('TIME', 'SESSION', 'PT')),
    CONSTRAINT ck_memberships_status CHECK (status IN ('PENDING', 'UPCOMING', 'ACTIVE', 'FROZEN', 'EXPIRED', 'TRANSFERRED', 'CANCELLED')),
    CONSTRAINT ck_memberships_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_memberships_remaining_sessions CHECK (remaining_sessions IS NULL OR remaining_sessions >= 0),
    CONSTRAINT ck_memberships_freeze_count CHECK (freeze_count >= 0),
    CONSTRAINT ck_memberships_version CHECK (version >= 0)
);

CREATE INDEX ix_memberships_member_status ON memberships (member_id, status);
CREATE INDEX ix_memberships_end_date ON memberships (end_date);
CREATE INDEX ix_memberships_upcoming ON memberships (start_date) WHERE status = 'UPCOMING';
CREATE UNIQUE INDEX uq_one_active_per_type ON memberships (member_id, package_type, branch_id) NULLS NOT DISTINCT WHERE status IN ('ACTIVE', 'FROZEN');
CREATE INDEX ix_memberships_package_id ON memberships (package_id);
CREATE INDEX ix_memberships_invoice_item_id ON memberships (invoice_item_id);
CREATE INDEX ix_memberships_sold_branch ON memberships (sold_branch_id);

