-- Demo ban hang, thanh toan va goi TIME duoc kich hoat.
-- Luu y: file R__ chi chay lai khi noi dung thay doi, nen cac ngay tuong doi ((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date) duoc chot tai thoi diem Flyway chay.
INSERT INTO products (sku,name,price,unit) VALUES
('SP01','Nước khoáng 500ml',10000,'Chai'),('SP02','Khăn tắm',50000,'Cái')
ON CONFLICT (sku) DO NOTHING;
INSERT INTO inventories (branch_id,product_id,quantity,min_quantity)
SELECT b.id,p.id,100,10 FROM branches b CROSS JOIN products p
WHERE b.code='CN01' AND p.sku IN ('SP01','SP02')
ON CONFLICT (branch_id,product_id) DO NOTHING;
INSERT INTO promotions (branch_id,code,type,value,max_discount,min_amount,start_at,end_at,usage_limit)
SELECT b.id,'OPENING20','PERCENT',20,100000,0,now(),now()+INTERVAL '30 days',100
FROM branches b WHERE b.code='CN01' ON CONFLICT (code) DO NOTHING;

INSERT INTO invoices (code,branch_id,member_id,created_by,source,subtotal,discount,total,status,paid_at)
SELECT 'INV2611001',b.id,m.id,u.id,'COUNTER',p.price,0,p.price,'PAID',now()
FROM branches b JOIN members m ON m.home_branch_id=b.id
JOIN users u ON u.email='receptionist@example.com' JOIN packages p ON p.code='TIME_1M'
WHERE b.code='CN01' AND m.member_code='HV999001' ON CONFLICT (code) DO NOTHING;

INSERT INTO invoice_items (invoice_id,item_type,package_id,description,quantity,unit_price,amount,discount_amount)
SELECT i.id,'PACKAGE',p.id,p.name,1,p.price,p.price,0 FROM invoices i JOIN packages p ON p.code='TIME_1M'
WHERE i.code='INV2611001' AND NOT EXISTS
(SELECT 1 FROM invoice_items ii WHERE ii.invoice_id=i.id AND ii.item_type='PACKAGE' AND ii.package_id=p.id);

INSERT INTO payments (invoice_id,method,amount,status,received_by,paid_at)
SELECT i.id,'CASH',i.total,'CONFIRMED',u.id,i.paid_at FROM invoices i
JOIN users u ON u.email='receptionist@example.com'
WHERE i.code='INV2611001' AND i.status='PAID' AND i.paid_at IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM payments pay WHERE pay.invoice_id=i.id);

INSERT INTO memberships (member_id,package_id,package_type,branch_id,sold_branch_id,invoice_item_id,
start_date,end_date,total_sessions,remaining_sessions,status)
SELECT m.id,p.id,p.type,p.branch_id,b.id,ii.id,
(i.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date,
(i.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + p.duration_days - 1,
NULL,NULL,'ACTIVE'
FROM invoices i JOIN invoice_items ii ON ii.invoice_id=i.id
JOIN packages p ON p.id=ii.package_id JOIN members m ON m.id=i.member_id
JOIN branches b ON b.id=i.branch_id
WHERE i.code='INV2611001' AND ii.item_type='PACKAGE' AND p.code='TIME_1M' AND p.type='TIME'
AND i.status='PAID' AND EXISTS
(SELECT 1 FROM payments pay WHERE pay.invoice_id=i.id AND pay.status='CONFIRMED' AND pay.amount=i.total)
AND NOT EXISTS (SELECT 1 FROM memberships ms WHERE ms.invoice_item_id=ii.id);

-- Bo sung S2-29.1: hoa don HD999001..HD999011, dong hoa don, phieu thu va goi tap cho cac truong hop F3-F10.
INSERT INTO invoices (code,branch_id,member_id,created_by,source,subtotal,discount,total,status,paid_at)
SELECT v.inv_code,b.id,m.id,u.id,'COUNTER',p.price,0,p.price,v.inv_status,
CASE WHEN v.inv_status='PAID' THEN now() ELSE NULL END
FROM (VALUES
 ('HD999001','CN01','HV999001','receptionist@example.com','PT_10','PAID'),
 ('HD999002','CN01','HV999004','receptionist@example.com','TIME_1M','PAID'),
 ('HD999003','CN01','HV999004','receptionist@example.com','SES_10','PAID'),
 ('HD999004','CN01','HV999005','receptionist@example.com','SES_10','PAID'),
 ('HD999005','CN01','HV999006','receptionist@example.com','PT_10','PAID'),
 ('HD999006','CN01','HV999007','receptionist@example.com','TIME_1M','PAID'),
 ('HD999007','CN01','HV999008','receptionist@example.com','TIME_1M','PAID'),
 ('HD999008','CN01','HV999008','receptionist@example.com','TIME_1M','PAID'),
 ('HD999009','CN01','HV999009','receptionist@example.com','TIME_1M','PAID'),
 ('HD999010','CN02','HV999012','receptionist2@example.com','TIME_1M','PAID'),
 ('HD999011','CN01','HV999011','receptionist@example.com','TIME_1M','PENDING')
) AS v(inv_code,branch_code,member_code,staff_email,pkg_code,inv_status)
JOIN branches b ON b.code=v.branch_code
JOIN members m ON m.member_code=v.member_code
JOIN users u ON u.email=v.staff_email
JOIN packages p ON p.code=v.pkg_code
ON CONFLICT (code) DO NOTHING;

INSERT INTO invoice_items (invoice_id,item_type,package_id,description,quantity,unit_price,amount,discount_amount)
SELECT i.id,'PACKAGE',p.id,p.name,1,p.price,p.price,0
FROM (VALUES
 ('HD999001','PT_10'),
 ('HD999002','TIME_1M'),
 ('HD999003','SES_10'),
 ('HD999004','SES_10'),
 ('HD999005','PT_10'),
 ('HD999006','TIME_1M'),
 ('HD999007','TIME_1M'),
 ('HD999008','TIME_1M'),
 ('HD999009','TIME_1M'),
 ('HD999010','TIME_1M'),
 ('HD999011','TIME_1M')
) AS v(inv_code,pkg_code)
JOIN invoices i ON i.code=v.inv_code
JOIN packages p ON p.code=v.pkg_code
WHERE NOT EXISTS (SELECT 1 FROM invoice_items ii WHERE ii.invoice_id=i.id);

INSERT INTO payments (invoice_id,method,amount,status,received_by,paid_at)
SELECT i.id,'CASH',i.total,'CONFIRMED',i.created_by,i.paid_at
FROM invoices i
WHERE i.code IN ('HD999001','HD999002','HD999003','HD999004','HD999005','HD999006','HD999007','HD999008','HD999009','HD999010')
AND i.status='PAID' AND i.paid_at IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM payments pay WHERE pay.invoice_id=i.id);

INSERT INTO memberships (member_id,package_id,package_type,branch_id,sold_branch_id,invoice_item_id,
start_date,end_date,total_sessions,remaining_sessions,status)
SELECT i.member_id,p.id,p.type,p.branch_id,i.branch_id,ii.id,
(now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + v.start_offset,
(now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date + v.start_offset + p.duration_days - 1,
p.session_count,v.rem_sessions,v.ms_status
FROM (VALUES
 ('HD999001',0,10,'ACTIVE'),
 ('HD999002',0,NULL::integer,'ACTIVE'),
 ('HD999003',0,10,'ACTIVE'),
 ('HD999004',0,1,'ACTIVE'),
 ('HD999005',0,10,'ACTIVE'),
 ('HD999006',-26,NULL::integer,'ACTIVE'),
 ('HD999007',-20,NULL::integer,'ACTIVE'),
 ('HD999008',10,NULL::integer,'UPCOMING'),
 ('HD999009',0,NULL::integer,'ACTIVE'),
 ('HD999010',0,NULL::integer,'ACTIVE'),
 ('HD999011',0,NULL::integer,'PENDING')
) AS v(inv_code,start_offset,rem_sessions,ms_status)
JOIN invoices i ON i.code=v.inv_code
JOIN invoice_items ii ON ii.invoice_id=i.id
JOIN packages p ON p.id=ii.package_id
WHERE NOT EXISTS (SELECT 1 FROM memberships ms WHERE ms.invoice_item_id=ii.id);

-- Bo sung S2-09.2 (G2): cau hinh tai khoan ngan hang VietQR mac dinh toan chuoi (gia tri gia cho moi truong demo).
INSERT INTO system_configs (branch_id, config_key, config_value, description)
VALUES
    (NULL, 'BANK_ID', 'MB', 'Mã ngân hàng nhận chuyển khoản VietQR mặc định toàn chuỗi (giá trị mẫu)'),
    (NULL, 'BANK_ACCOUNT_NO', '0987654321', 'Số tài khoản nhận chuyển khoản VietQR mặc định toàn chuỗi (giá trị mẫu)'),
    (NULL, 'BANK_ACCOUNT_NAME', 'CONG TY GYM NHOM 6', 'Tên chủ tài khoản nhận chuyển khoản VietQR mặc định toàn chuỗi (giá trị mẫu)')
ON CONFLICT (branch_id, config_key) DO NOTHING;

-- Sua du lieu mau da nap truoc ban sua: memberships.branch_id phai chep tu packages.branch_id
-- (rong = goi toan chuoi, ERD), khong phai chi nhanh ban. Chi sua goi mau (hoa don mau INV2611001, HD999xxx);
-- bo qua neu hoi vien da co goi ACTIVE/FROZEN cung loai, cung pham vi (tranh vi pham uq_one_active_per_type).
UPDATE memberships ms SET branch_id = p.branch_id
FROM packages p, invoice_items ii, invoices i
WHERE p.id = ms.package_id AND ii.id = ms.invoice_item_id AND i.id = ii.invoice_id
AND (i.code = 'INV2611001' OR i.code LIKE 'HD999%')
AND ms.branch_id IS DISTINCT FROM p.branch_id
AND NOT (ms.status IN ('ACTIVE','FROZEN') AND EXISTS (
  SELECT 1 FROM memberships o WHERE o.id <> ms.id AND o.member_id = ms.member_id
  AND o.package_type = ms.package_type AND o.branch_id IS NOT DISTINCT FROM p.branch_id
  AND o.status IN ('ACTIVE','FROZEN')));
