-- Nhóm 6: Bán hàng và thanh toán. Tạo bảng payments.
-- Khóa ngoại liên nhóm được thêm ở V8_03.

CREATE TABLE payments (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id bigint NOT NULL,
    method varchar(20) NOT NULL,
    amount integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'CONFIRMED',
    transaction_ref varchar(100),
    received_by bigint NOT NULL,
    paid_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_payments_method CHECK (method IN ('CASH', 'BANK_TRANSFER')),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_status CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);
CREATE INDEX ix_payments_invoice_id ON payments(invoice_id);
CREATE INDEX ix_payments_received_by ON payments(received_by);
