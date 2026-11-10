-- Nhóm 6: Bán hàng và thanh toán (F5, F7, F12, F14)
-- Bảng invoices: Hóa đơn

CREATE TABLE invoices (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(30) NOT NULL UNIQUE,
    branch_id bigint NOT NULL,
    member_id bigint,
    created_by bigint NOT NULL,
    source varchar(20) NOT NULL,
    promotion_id bigint,
    subtotal integer NOT NULL,
    discount integer NOT NULL DEFAULT 0,
    total integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    paid_at timestamptz,
    refunded_by bigint,
    refunded_at timestamptz,
    refund_reason text,
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_invoices_source CHECK (source IN ('COUNTER', 'MEMBER_PORTAL')),
    CONSTRAINT ck_invoices_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED', 'REFUNDED')),
    CONSTRAINT ck_invoices_amounts CHECK (
        subtotal >= 0 AND discount >= 0 AND total >= 0 AND total = subtotal - discount
    ),
    CONSTRAINT fk_invoices_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id)
);
CREATE INDEX ix_invoices_branch_created_at ON invoices (branch_id, created_at);
CREATE INDEX ix_invoices_member_created_at ON invoices (member_id, created_at);
CREATE INDEX ix_invoices_pending_portal ON invoices (created_at)
    WHERE status = 'PENDING' AND source = 'MEMBER_PORTAL';
