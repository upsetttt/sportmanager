-- MIGRATION ADDITIVE: sao chép dữ liệu từ schema cũ `sannhaminh` sang `sannhaminh_new`.
-- Yêu cầu: đã backup database cũ; đã tạo schema mới và chạy backend ít nhất một lần để tạo bảng.
-- Script không DROP/ALTER/XÓA dữ liệu trong schema cũ. Có thể chạy lại nhờ INSERT IGNORE.
-- Nếu database cũ có tên khác, thay tiền tố `sannhaminh.` trong toàn bộ file.

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Tài khoản cũ: giữ ID và password hash. account_access.enabled=false -> LOCKED.
INSERT IGNORE INTO sannhaminh_new.accounts(id,full_name,email,phone,password_hash,role,status,created_at)
SELECT a.id,a.full_name,a.email,a.phone,a.password_hash,
       CASE WHEN UPPER(a.role)='OWNER' THEN 'OWNER' WHEN UPPER(a.role)='ADMIN' THEN 'ADMIN' ELSE 'PLAYER' END,
       CASE WHEN COALESCE(ax.enabled,1)=0 THEN 'LOCKED' ELSE 'ACTIVE' END,
       COALESCE(a.created_at,CURRENT_TIMESTAMP)
FROM sannhaminh.accounts a
LEFT JOIN sannhaminh.account_access ax ON ax.account_id=a.id;

-- 2. Khách đặt sân chưa có tài khoản: tạo hồ sơ LOCKED để giữ liên kết lịch sử.
-- Họ cần được Admin cấp lại mật khẩu trước khi đăng nhập.
INSERT IGNORE INTO sannhaminh_new.accounts(full_name,email,phone,password_hash,role,status)
SELECT DISTINCT b.customer_name,b.email,b.phone,'LEGACY_ACCOUNT_RESET_REQUIRED','PLAYER','LOCKED'
FROM sannhaminh.bookings b
LEFT JOIN sannhaminh_new.accounts a ON LOWER(a.email)=LOWER(b.email)
WHERE a.id IS NULL AND b.email IS NOT NULL AND b.email<>'';

-- 3. Danh mục: lấy theo sport của sân cũ.
INSERT IGNORE INTO sannhaminh_new.categories(name,description)
SELECT DISTINCT TRIM(sport),CONCAT('Danh mục được chuyển từ dữ liệu cũ: ',TRIM(sport))
FROM sannhaminh.venues WHERE sport IS NOT NULL AND TRIM(sport)<>'';

-- 4. Sân: giữ ID, chủ sân, giá, ảnh, rating và trạng thái active.
INSERT IGNORE INTO sannhaminh_new.venues(id,owner_id,category_id,name,description,address,district,city,image_url,price_per_hour,amenities,status,rating,created_at)
SELECT v.id,COALESCE(v.owner_id,(SELECT id FROM sannhaminh_new.accounts WHERE role='ADMIN' ORDER BY id LIMIT 1)),
       c.id,v.name,NULL,v.address,NULL,'Hà Nội',v.image_url,v.price_per_hour,NULL,
       CASE WHEN v.active=1 THEN 'ACTIVE' ELSE 'ARCHIVED' END,COALESCE(v.rating,0),CURRENT_TIMESTAMP
FROM sannhaminh.venues v
LEFT JOIN sannhaminh_new.categories c ON c.name=TRIM(v.sport)
WHERE COALESCE(v.owner_id,(SELECT id FROM sannhaminh_new.accounts WHERE role='ADMIN' ORDER BY id LIMIT 1)) IS NOT NULL;

-- 5. Lịch hoạt động: chuyển tên cột opening_time/closing_time sang open_time/close_time.
INSERT IGNORE INTO sannhaminh_new.venue_schedules(venue_id,day_of_week,open_time,close_time,slot_minutes,is_closed)
SELECT venue_id,day_of_week,opening_time,closing_time,slot_minutes,is_closed
FROM sannhaminh.venue_schedules;

-- 6. Booking: liên kết lại khách theo email và chủ sân theo venue.owner_id.
INSERT IGNORE INTO sannhaminh_new.bookings(id,booking_code,player_id,owner_id,venue_id,play_date,start_time,end_time,total_hours,total_amount,status,customer_note,created_at)
SELECT b.id,CONCAT('LEGACY',b.id),a.id,v.owner_id,b.venue_id,b.booking_date,b.start_time,b.end_time,
       GREATEST(1,TIMESTAMPDIFF(HOUR,b.start_time,b.end_time)),b.total_price,
       CASE UPPER(b.status) WHEN 'CONFIRMED' THEN 'CONFIRMED' WHEN 'COMPLETED' THEN 'COMPLETED'
            WHEN 'CANCELLED' THEN 'CANCELLED' WHEN 'REJECTED' THEN 'REJECTED' ELSE 'PENDING_OWNER' END,
       'Dữ liệu được chuyển từ phiên bản trước',COALESCE(b.created_at,CURRENT_TIMESTAMP)
FROM sannhaminh.bookings b
JOIN sannhaminh_new.accounts a ON LOWER(a.email)=LOWER(b.email)
JOIN sannhaminh_new.venues v ON v.id=b.venue_id;

-- 7. Thanh toán: giữ ID, số tiền, phương thức và trạng thái.
INSERT IGNORE INTO sannhaminh_new.payments(id,booking_id,payer_id,method,amount,status,transaction_ref,created_at,updated_at)
SELECT p.id,p.booking_id,a.id,
       CASE UPPER(p.payment_method) WHEN 'CASH' THEN 'CASH' WHEN 'VNPAY' THEN 'VNPAY' WHEN 'MOMO' THEN 'MOMO' ELSE 'BANK_TRANSFER' END,
       p.amount,
       CASE UPPER(p.status) WHEN 'PAID' THEN 'PAID' WHEN 'REFUND_PENDING' THEN 'REFUND_PENDING'
            WHEN 'REFUNDED' THEN 'REFUNDED' WHEN 'CANCELLED' THEN 'CANCELLED' ELSE 'PENDING' END,
       p.transaction_reference,COALESCE(p.created_at,CURRENT_TIMESTAMP),COALESCE(p.updated_at,CURRENT_TIMESTAMP)
FROM sannhaminh.payments p
JOIN sannhaminh.bookings b ON b.id=p.booking_id
JOIN sannhaminh_new.accounts a ON LOWER(a.email)=LOWER(p.payer_email)
JOIN sannhaminh_new.bookings nb ON nb.id=p.booking_id;

-- 8. Đánh giá và phản hồi chủ sân.
INSERT IGNORE INTO sannhaminh_new.reviews(id,booking_id,venue_id,player_id,rating,content,owner_reply,status,created_at)
SELECT r.id,r.booking_id,r.venue_id,a.id,r.rating,r.comment,rr.reply,'VISIBLE',COALESCE(r.created_at,CURRENT_TIMESTAMP)
FROM sannhaminh.reviews r
JOIN sannhaminh_new.accounts a ON LOWER(a.email)=LOWER(r.customer_email)
JOIN sannhaminh_new.bookings b ON b.id=r.booking_id
LEFT JOIN sannhaminh.review_replies rr ON rr.review_id=r.id;

-- 9. Yêu thích và thông báo.
INSERT IGNORE INTO sannhaminh_new.favorites(player_id,venue_id,created_at)
SELECT f.account_id,f.venue_id,COALESCE(f.created_at,CURRENT_TIMESTAMP)
FROM sannhaminh.favorites f
JOIN sannhaminh_new.accounts a ON a.id=f.account_id
JOIN sannhaminh_new.venues v ON v.id=f.venue_id;

INSERT IGNORE INTO sannhaminh_new.notifications(id,account_id,title,message,is_read,created_at)
SELECT n.id,a.id,n.title,n.message,n.is_read,COALESCE(n.created_at,CURRENT_TIMESTAMP)
FROM sannhaminh.notifications n
JOIN sannhaminh_new.accounts a ON LOWER(a.email)=LOWER(n.account_email);

SET FOREIGN_KEY_CHECKS = 1;

-- Kiểm tra số lượng bản ghi sau migration.
SELECT 'accounts' AS table_name,COUNT(*) AS row_count FROM sannhaminh_new.accounts
UNION ALL SELECT 'venues',COUNT(*) FROM sannhaminh_new.venues
UNION ALL SELECT 'bookings',COUNT(*) FROM sannhaminh_new.bookings
UNION ALL SELECT 'payments',COUNT(*) FROM sannhaminh_new.payments
UNION ALL SELECT 'reviews',COUNT(*) FROM sannhaminh_new.reviews
UNION ALL SELECT 'favorites',COUNT(*) FROM sannhaminh_new.favorites
UNION ALL SELECT 'notifications',COUNT(*) FROM sannhaminh_new.notifications;
