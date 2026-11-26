-- ADR 0011, US-F2.4.2 (#183). V9_03 đã có trên main nên dùng số tiếp theo.
ALTER TABLE members DROP COLUMN avatar_url;

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
