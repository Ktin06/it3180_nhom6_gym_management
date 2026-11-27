ALTER TABLE class_sessions ADD COLUMN class_type_id bigint;

-- Buổi cũ chưa lưu loại lớp riêng; dùng loại hiện có của lịch lặp tại thời điểm nâng cấp.
UPDATE class_sessions AS session
SET class_type_id = schedule.class_type_id
FROM class_schedules AS schedule
WHERE session.class_schedule_id = schedule.id;

ALTER TABLE class_sessions
    ALTER COLUMN class_type_id SET NOT NULL,
    ADD CONSTRAINT fk_class_sessions_class_type FOREIGN KEY (class_type_id) REFERENCES class_types (id);

-- Backend cũ vẫn có thể sinh buổi trong lúc phiên bản mới được triển khai.
CREATE FUNCTION fill_class_session_type() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    SELECT class_type_id INTO NEW.class_type_id
    FROM class_schedules
    WHERE id = NEW.class_schedule_id;
    RETURN NEW;
END;
$$;

CREATE TRIGGER fill_class_session_type
BEFORE INSERT ON class_sessions
FOR EACH ROW WHEN (NEW.class_type_id IS NULL)
EXECUTE FUNCTION fill_class_session_type();
