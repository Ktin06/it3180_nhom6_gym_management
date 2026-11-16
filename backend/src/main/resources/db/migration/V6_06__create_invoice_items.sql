-- Nhóm 6: Bán hàng và thanh toán
-- Bảng invoice_items: Dòng hóa đơn

CREATE TABLE invoice_items (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id bigint NOT NULL,
    item_type varchar(20) NOT NULL,
    package_id bigint,
    product_id bigint,
    description varchar(255) NOT NULL,
    quantity integer NOT NULL DEFAULT 1,
    unit_price integer NOT NULL,
    amount integer NOT NULL,
    discount_amount integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_invoice_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_invoice_items_type CHECK (item_type IN ('PACKAGE', 'PRODUCT', 'TRANSFER_FEE')),
    CONSTRAINT ck_invoice_items_type_reference CHECK (
        (item_type = 'PACKAGE' AND package_id IS NOT NULL AND product_id IS NULL)
        OR (item_type = 'PRODUCT' AND product_id IS NOT NULL AND package_id IS NULL)
        OR (item_type = 'TRANSFER_FEE' AND package_id IS NULL AND product_id IS NULL)
    ),
    CONSTRAINT ck_invoice_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_invoice_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_invoice_items_amount CHECK (
        amount::bigint = quantity::bigint * unit_price::bigint
    ),
    CONSTRAINT ck_invoice_items_discount CHECK (
        discount_amount >= 0 AND discount_amount <= amount
    )
);
CREATE INDEX ix_invoice_items_invoice_id ON invoice_items (invoice_id);
