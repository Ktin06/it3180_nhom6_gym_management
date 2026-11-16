-- Danh muc huy hieu demo.
INSERT INTO badges (code,name,description,rule) VALUES
('BADGE_CHECKIN_FIRST','Lần đầu đi tập','Check-in lần đầu','{"type":"CHECKIN_COUNT","count":1}'::jsonb),
('BADGE_STREAK_07','Đi tập liên tục 7 ngày','Chuỗi 7 ngày','{"type":"STREAK","days":7}'::jsonb),
('BADGE_STREAK_30','Đi tập liên tục 30 ngày','Chuỗi 30 ngày','{"type":"STREAK","days":30}'::jsonb),
('BADGE_STREAK_90','Đi tập liên tục 90 ngày','Chuỗi 90 ngày','{"type":"STREAK","days":90}'::jsonb)
ON CONFLICT (code) DO NOTHING;

-- Check-in TIME, khong tru buoi. Thoi diem duoc tao theo ngay bat dau goi.
INSERT INTO checkins (member_id,membership_id,branch_id,created_by,method,checkin_date,session_deducted,checked_in_at)
SELECT m.id,ms.id,b.id,u.id,'MANUAL',ms.start_date,false,
(ms.start_date::timestamp + TIME '17:30') AT TIME ZONE 'Asia/Ho_Chi_Minh'
FROM members m JOIN memberships ms ON ms.member_id=m.id JOIN branches b ON b.id=ms.branch_id
JOIN users u ON u.email='receptionist@example.com'
WHERE m.member_code='HV999001' AND b.code='CN01' AND ms.package_type='TIME' AND ms.status='ACTIVE'
AND ms.start_date <= (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
AND ms.end_date >= (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date
AND TIME '17:30' >= b.open_time AND TIME '17:30' < b.close_time
AND NOT EXISTS (SELECT 1 FROM checkins c WHERE c.member_id=m.id AND c.membership_id=ms.id
 AND c.checkin_date=ms.start_date);

INSERT INTO member_badges (member_id,badge_id,earned_at)
SELECT m.id,b.id,MIN(c.checked_in_at) FROM members m JOIN checkins c ON c.member_id=m.id
JOIN badges b ON b.code='BADGE_CHECKIN_FIRST' WHERE m.member_code='HV999001'
GROUP BY m.id,b.id ON CONFLICT (member_id,badge_id) DO NOTHING;
