-- Nhóm 6: Bán hàng và thanh toán (F12)
-- Bảng inventories: Tồn kho theo chi nhánh

CREATE TABLE inventories (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity integer NOT NULL DEFAULT 0,
    min_quantity integer NOT NULL DEFAULT 0,
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventories_branch_product UNIQUE (branch_id, product_id),
    CONSTRAINT ck_inventories_quantity CHECK (quantity >= 0),
    CONSTRAINT ck_inventories_min_quantity CHECK (min_quantity >= 0),
    CONSTRAINT fk_inventories_product
    FOREIGN KEY (product_id)
    REFERENCES products(id)
);
