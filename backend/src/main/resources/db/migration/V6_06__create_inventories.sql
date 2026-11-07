-- Nhóm 6: Bán hàng và thanh toán. Tạo bảng inventories.
-- Khóa ngoại liên nhóm được thêm ở V8_03.

CREATE TABLE inventories (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    branch_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity integer NOT NULL DEFAULT 0,
    min_quantity integer NOT NULL DEFAULT 0,
    version integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_inventories_quantity_nonnegative CHECK (quantity >= 0),
    CONSTRAINT ck_inventories_min_quantity_nonnegative CHECK (min_quantity >= 0),
    CONSTRAINT uq_inventories_branch_product UNIQUE (branch_id, product_id),
    CONSTRAINT fk_inventories_product FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE INDEX ix_inventories_product_id ON inventories(product_id);
