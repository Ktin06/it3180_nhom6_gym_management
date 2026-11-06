-- Nhan vien demo. Khong gan ID truc tiep.
INSERT INTO staff (user_id,branch_id,staff_code,full_name,phone,base_salary,hourly_rate,commission_rate,specialties)
SELECT u.id,b.id,v.staff_code,v.full_name,v.phone,v.base_salary,v.hourly_rate,v.commission_rate,v.specialties
FROM (VALUES
 ('manager@example.com','NV999001','Quản lý 1','0911000001',15000000,NULL::integer,NULL::numeric,NULL::varchar),
 ('receptionist@example.com','NV999002','Lễ tân 1','0911000002',7000000,NULL::integer,NULL::numeric,NULL::varchar),
 ('trainer1@example.com','PT999001','Huấn luyện viên Yoga','0911000003',5000000,100000,0.10::numeric,'Yoga, Pilates'),
 ('trainer2@example.com','PT999002','Huấn luyện viên Gym','0911000004',5000000,150000,0.15::numeric,'Gym, Boxing')
) AS v(email,staff_code,full_name,phone,base_salary,hourly_rate,commission_rate,specialties)
JOIN users u ON u.email=v.email CROSS JOIN branches b WHERE b.code='CN01'
ON CONFLICT DO NOTHING;

INSERT INTO rooms (branch_id,name,type,capacity)
SELECT b.id,v.name,v.type,v.capacity FROM branches b CROSS JOIN
(VALUES ('Studio Yoga','STUDIO',30),('Khu Gym','GYM_FLOOR',100)) AS v(name,type,capacity)
WHERE b.code='CN01' ON CONFLICT (branch_id,name) DO NOTHING;

INSERT INTO class_types (name,description) VALUES
('Yoga Cơ bản','Yoga cho người mới'),('Zumba Dance','Lớp nhảy Zumba')
ON CONFLICT (name) DO NOTHING;

-- Lich lap hang tuan, chi them neu chua ton tai.
INSERT INTO class_schedules (branch_id,class_type_id,trainer_id,room_id,day_of_week,start_time,end_time,capacity,valid_from,status)
SELECT b.id,ct.id,s.id,r.id,v.dow,v.starts,v.ends,25,(now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date,'ACTIVE'
FROM (VALUES ('Yoga Cơ bản','PT999001',2,TIME '18:00',TIME '19:00'),
             ('Zumba Dance','PT999002',4,TIME '19:00',TIME '20:00')) AS v(class_name,staff_code,dow,starts,ends)
JOIN branches b ON b.code='CN01' JOIN class_types ct ON ct.name=v.class_name
JOIN staff s ON s.staff_code=v.staff_code JOIN rooms r ON r.branch_id=b.id AND r.name='Studio Yoga'
WHERE NOT EXISTS (SELECT 1 FROM class_schedules cs WHERE cs.branch_id=b.id AND cs.class_type_id=ct.id
 AND cs.trainer_id=s.id AND cs.room_id=r.id AND cs.day_of_week=v.dow AND cs.start_time=v.starts);

INSERT INTO trainer_availabilities (trainer_id,branch_id,day_of_week,start_time,end_time)
SELECT s.id,b.id,v.dow,v.starts,v.ends
FROM (VALUES ('PT999001',3,TIME '08:00',TIME '12:00'),('PT999002',5,TIME '13:00',TIME '17:00')) AS v(staff_code,dow,starts,ends)
JOIN staff s ON s.staff_code=v.staff_code JOIN branches b ON b.code='CN01'
WHERE NOT EXISTS (SELECT 1 FROM trainer_availabilities a WHERE a.trainer_id=s.id AND a.branch_id=b.id
 AND a.day_of_week=v.dow AND a.start_time=v.starts AND a.end_time=v.ends);
