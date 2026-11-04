-- Nhóm 3: Hội viên và gói tập
-- Bảng membership_transfers: chuyển nhượng gói tập (F15)
-- Lưu ý: các khóa ngoại liên nhóm (invoice_id -> invoices, approved_by -> users)
-- được gắn ở cuối dải V8_xx

CREATE TABLE membership_transfers (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    from_membership_id bigint NOT NULL,
    to_membership_id bigint,
    from_member_id bigint NOT NULL,
    to_member_id bigint NOT NULL,
    invoice_id bigint,
    fee integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    approved_by bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_membership_transfers_from_membership FOREIGN KEY (from_membership_id) REFERENCES memberships (id),
    CONSTRAINT fk_membership_transfers_to_membership FOREIGN KEY (to_membership_id) REFERENCES memberships (id),
    CONSTRAINT fk_membership_transfers_from_member FOREIGN KEY (from_member_id) REFERENCES members (id),
    CONSTRAINT fk_membership_transfers_to_member FOREIGN KEY (to_member_id) REFERENCES members (id),
    CONSTRAINT ck_membership_transfers_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_membership_transfers_different_members CHECK (from_member_id <> to_member_id),
    CONSTRAINT ck_membership_transfers_fee CHECK (fee >= 0),
    CONSTRAINT ck_membership_transfers_completed_has_to_membership CHECK (
        (status = 'COMPLETED' AND to_membership_id IS NOT NULL) OR (status <> 'COMPLETED')
    )
);

CREATE INDEX ix_membership_transfers_from_member ON membership_transfers (from_member_id);
CREATE INDEX ix_membership_transfers_to_member ON membership_transfers (to_member_id);
CREATE INDEX ix_membership_transfers_from_membership ON membership_transfers (from_membership_id);
CREATE INDEX ix_membership_transfers_to_membership ON membership_transfers (to_membership_id);
CREATE INDEX ix_membership_transfers_invoice_id ON membership_transfers (invoice_id);

