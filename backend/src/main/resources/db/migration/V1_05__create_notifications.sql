-- Nhóm 1: Tài khoản và hệ thống
-- Bảng notifications: thông báo trong web và email (F10)

CREATE TABLE notifications (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL,
    type varchar(50) NOT NULL,
    title varchar(255) NOT NULL,
    content text,
    channel varchar(20) NOT NULL,
    related_entity varchar(50),
    related_id bigint,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    scheduled_at timestamptz,
    sent_at timestamptz,
    read_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_notifications_channel CHECK (channel IN ('IN_APP', 'EMAIL')),
    CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX ix_notifications_user_read_at ON notifications (user_id, read_at);
CREATE INDEX ix_notifications_status_scheduled_at ON notifications (status, scheduled_at);
