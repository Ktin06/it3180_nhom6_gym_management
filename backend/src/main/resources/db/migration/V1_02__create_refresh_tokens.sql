-- Nhóm 1: Tài khoản và hệ thống
-- Bảng refresh_tokens: phiên đăng nhập dài hạn để làm mới token và đăng xuất (F1, S2)

CREATE TABLE refresh_tokens (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL,
    token_hash varchar(255) NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
