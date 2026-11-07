-- Nhóm 6: Bán hàng và thanh toán. Tạo bảng products.
-- Khóa ngoại liên nhóm được thêm ở V8_03.

CREATE TABLE products (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku varchar(50) NOT NULL UNIQUE,
    name varchar(255) NOT NULL,
    price integer NOT NULL,
    unit varchar(20),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_products_price_nonnegative CHECK (price >= 0),
    CONSTRAINT ck_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
