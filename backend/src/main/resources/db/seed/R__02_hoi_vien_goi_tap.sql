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
