-- Nhóm 8: Gamification (F19)
-- Bảng member_badges: Huy hiệu hội viên đã đạt

CREATE TABLE member_badges (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id bigint NOT NULL,
    badge_id bigint NOT NULL,
    earned_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_member_badges_member_badge UNIQUE (member_id, badge_id),
    CONSTRAINT fk_member_badges_badge FOREIGN KEY (badge_id) REFERENCES badges (id)
);
