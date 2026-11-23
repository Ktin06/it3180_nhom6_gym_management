-- Ảnh đại diện hội viên lưu trong PostgreSQL theo ADR 0011 (F2, F4, việc S2-06, Issue #183).
-- Số V9_06, không phải V9_02 đã giữ: V9_03 đã chạy ở staging, Flyway từ chối số nhỏ hơn bản đã chạy.
-- Bỏ cột members.avatar_url để không lưu một thông tin ở hai nơi (quy tắc mã số 12)
-- và tạo bảng riêng member_avatars (mỗi hội viên tối đa một ảnh JPEG <= 100 KB, cạnh dài <= 256 px).

ALTER TABLE members DROP COLUMN IF EXISTS avatar_url;

CREATE TABLE member_avatars (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id bigint NOT NULL,
    content bytea NOT NULL,
    content_type varchar(50) NOT NULL DEFAULT 'image/jpeg',
    size_bytes integer NOT NULL,
    uploaded_by bigint NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_member_avatars_member_id UNIQUE (member_id),
    CONSTRAINT fk_member_avatars_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_member_avatars_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id),
    CONSTRAINT ck_member_avatars_content_type CHECK (content_type = 'image/jpeg'),
    CONSTRAINT ck_member_avatars_size_bytes CHECK (size_bytes > 0 AND size_bytes <= 102400)
);
