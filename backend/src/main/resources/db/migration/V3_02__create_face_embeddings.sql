-- Nhóm 3: Hội viên và gói tập
-- Bảng face_embeddings: vector khuôn mặt 128 chiều do face-api.js trích xuất (F20)
-- Lưu ý: khóa ngoại liên nhóm (created_by -> users) được gắn ở cuối dải V8_xx

CREATE TABLE face_embeddings (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id bigint NOT NULL,
    embedding real[] NOT NULL,
    model_version varchar(50),
    created_by bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_face_embeddings_member FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT ck_face_embeddings_cardinality CHECK (cardinality(embedding) = 128)
);

CREATE INDEX ix_face_embeddings_member_id ON face_embeddings (member_id);

