-- Nhóm 1: Tài khoản và hệ thống
-- Bảng password_resets: mã đặt lại mật khẩu dùng một lần (F1)

CREATE TABLE password_resets (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL,
    token_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    used_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_password_resets_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_resets_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_password_resets_user_id ON password_resets (user_id);
