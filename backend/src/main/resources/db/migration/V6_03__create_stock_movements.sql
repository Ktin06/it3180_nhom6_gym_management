-- Nhóm 6: Bán hàng và thanh toán (F12)
-- Bảng stock_movements: Nhập, xuất, điều chỉnh kho

CREATE TABLE stock_movements (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint NOT NULL,
    product_id bigint NOT NULL,
    invoice_id bigint,
    created_by bigint NOT NULL,
    type varchar(20) NOT NULL,
    quantity integer NOT NULL,
    unit_cost integer,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stock_movements_type CHECK (type IN ('IN', 'OUT', 'ADJUST')),
    CONSTRAINT ck_stock_movements_quantity CHECK (
        (type = 'ADJUST' AND quantity <> 0) OR quantity > 0
    ),
    CONSTRAINT ck_stock_movements_unit_cost CHECK (
        unit_cost IS NULL OR (type = 'IN' AND unit_cost >= 0)
    ),
    CONSTRAINT fk_stock_movements_product
    FOREIGN KEY (product_id)
    REFERENCES products(id)
);
CREATE INDEX ix_stock_movements_branch_product_created_at ON stock_movements (branch_id, product_id, created_at);
CREATE INDEX ix_stock_movements_invoice_id ON stock_movements (invoice_id);
