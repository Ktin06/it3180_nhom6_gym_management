-- Nhóm 1: Tài khoản và hệ thống
-- Bảng audit_logs: nhật ký hoạt động ghi nhận thay đổi dữ liệu quan trọng (F17, S9)
-- Lưu ý: khóa ngoại branch_id -> branches(id) thuộc nhóm 2, sẽ được gắn ở cuối dải V8_xx

CREATE TABLE audit_logs (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint,
    branch_id bigint,
    action varchar(50) NOT NULL,
    entity varchar(50) NOT NULL,
    entity_id bigint,
    old_value jsonb,
    new_value jsonb,
    ip varchar(45),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_audit_logs_entity ON audit_logs (entity, entity_id);
CREATE INDEX ix_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX ix_audit_logs_branch_created_at ON audit_logs (branch_id, created_at);
