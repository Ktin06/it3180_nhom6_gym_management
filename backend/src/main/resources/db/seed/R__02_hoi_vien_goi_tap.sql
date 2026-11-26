-- Goi 12 thang = 360 ngay theo quy uoc demo 30 ngay/thang.
INSERT INTO packages (code,name,type,duration_days,session_count,price,description,status) VALUES
('TIME_1M','Gói 1 tháng','TIME',30,NULL,500000,'Gói 30 ngày','ACTIVE'),
('TIME_12M','Gói 12 tháng','TIME',360,NULL,5000000,'Gói 360 ngày','ACTIVE'),
('SES_10','Gói 10 buổi','SESSION',90,10,800000,'10 buổi trong 90 ngày','ACTIVE'),
('PT_10','Gói 10 buổi PT','PT',90,10,3000000,'10 buổi PT trong 90 ngày','ACTIVE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO members (user_id,home_branch_id,member_code,full_name,gender,phone,status)
SELECT u.id,b.id,'HV999001','Nguyễn Văn A','MALE','0901111111','ACTIVE'
FROM users u CROSS JOIN branches b WHERE u.email='member1@example.com' AND b.code='CN01'
ON CONFLICT DO NOTHING;
INSERT INTO members (user_id,home_branch_id,member_code,full_name,gender,phone,status)
SELECT u.id,b.id,'HV999002','Trần Thị B','FEMALE','0902222222','ACTIVE'
FROM users u CROSS JOIN branches b WHERE u.email='member2@example.com' AND b.code='CN01'
ON CONFLICT DO NOTHING;
INSERT INTO members (user_id,home_branch_id,member_code,full_name,gender,phone,status)
SELECT NULL,b.id,'HV999003','Lê Khách Lẻ','MALE','0903333333','ACTIVE'
FROM branches b WHERE b.code='CN01'
ON CONFLICT DO NOTHING;

-- Bo sung S2-29.1: hoi vien CN01 (HV999004..HV999011) va CN02 (HV999012) cho cac truong hop F3-F10.
INSERT INTO members (user_id,home_branch_id,member_code,full_name,gender,phone,status)
SELECT u.id,b.id,v.member_code,v.full_name,v.gender,v.phone,v.status
FROM (VALUES
 ('member4@example.com','CN01','HV999004','Phạm Văn C (TIME + SESSION)','MALE','0904444444','ACTIVE'),
 ('member5@example.com','CN01','HV999005','Hoàng Thị D (SESSION còn 1 buổi)','FEMALE','0905555555','ACTIVE'),
 ('member6@example.com','CN01','HV999006','Đặng Văn E (chỉ có PT)','MALE','0906666666','ACTIVE'),
 ('member7@example.com','CN01','HV999007','Vũ Thị F (TIME còn 3 ngày)','FEMALE','0907777777','ACTIVE'),
 ('member8@example.com','CN01','HV999008','Bùi Văn G (TIME + UPCOMING nối tiếp)','MALE','0908888888','ACTIVE'),
 ('member9@example.com','CN01','HV999009','Đỗ Thị H (Hồ sơ INACTIVE còn gói ACTIVE)','FEMALE','0909999999','INACTIVE'),
 ('member11@example.com','CN01','HV999011','Lý Thị K (Hóa đơn và gói PENDING)','FEMALE','0901111113','ACTIVE'),
 ('member3@example.com','CN02','HV999012','Trịnh Văn L (Hội viên CN02)','MALE','0901111114','ACTIVE')
) AS v(email,branch_code,member_code,full_name,gender,phone,status)
JOIN users u ON u.email=v.email JOIN branches b ON b.code=v.branch_code
ON CONFLICT DO NOTHING;

INSERT INTO members (user_id,home_branch_id,member_code,full_name,gender,phone,status)
SELECT NULL,b.id,'HV999010','Ngô Văn I (Chưa có tài khoản, chưa có gói)','MALE','0901111112','ACTIVE'
FROM branches b WHERE b.code='CN01'
ON CONFLICT DO NOTHING;
