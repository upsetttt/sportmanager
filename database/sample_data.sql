-- Dữ liệu mẫu cho San Nha Minh (chạy SAU khi đã có schema: sannhaminh.sql hoặc backend tự tạo bảng)
-- Tài khoản mẫu:  owner1@sannhaminh.local / owner2@sannhaminh.local  -> mật khẩu Owner@123456
--                 player@sannhaminh.local                              -> mật khẩu Player@123456
-- Có thể chạy nhiều lần: dữ liệu dùng INSERT IGNORE / ID cố định.
USE sannhaminh_new;
SET NAMES utf8mb4;

INSERT IGNORE INTO categories(id,name,description) VALUES
 (1,'Bóng đá','Sân bóng đá mini'),(2,'Cầu lông','Sân cầu lông'),(3,'Tennis','Sân tennis'),(4,'Bóng rổ','Sân bóng rổ');

INSERT IGNORE INTO accounts(id,full_name,email,phone,password_hash,role,status) VALUES
 (101,'Nguyễn Văn Chủ Sân','owner1@sannhaminh.local','0901000001','$2b$12$fHIIZ1LsEgNDAOdJFgpd7OYWObVym/NuKriecXQ8WUasWcd2QqTAu','OWNER','ACTIVE'),
 (102,'Trần Thị Chủ Sân','owner2@sannhaminh.local','0901000002','$2b$12$fHIIZ1LsEgNDAOdJFgpd7OYWObVym/NuKriecXQ8WUasWcd2QqTAu','OWNER','ACTIVE'),
 (103,'Lê Văn Người Chơi','player@sannhaminh.local','0902000003','$2b$12$bqtKQ6MUfc.CHfscs0hVo.O4Zz7.8PyW2A9MCQGOwA1qB3paKUdqi','PLAYER','ACTIVE');

INSERT IGNORE INTO venues(id,owner_id,category_id,name,description,address,district,city,image_url,price_per_hour,amenities,status,rating) VALUES
 (1,101,1,'Sân bóng đá mini Cầu Giấy','Sân cỏ nhân tạo 5 người, đèn chiếu sáng LED, có bãi giữ xe.','12 Trần Thái Tông','Cầu Giấy','Hà Nội','https://picsum.photos/seed/snm1/800/500',300000,'["Cỏ nhân tạo","Đèn LED","Bãi giữ xe","Phòng thay đồ"]','ACTIVE',4.7),
 (2,101,1,'Sân bóng Thống Nhất','Sân 7 người rộng rãi, phù hợp đá giao hữu và giải phong trào.','Số 1 Đại Cồ Việt','Hai Bà Trưng','Hà Nội','https://picsum.photos/seed/snm2/800/500',500000,'["Cỏ nhân tạo","Căng tin","Bãi giữ xe","Nước uống"]','ACTIVE',4.5),
 (3,102,1,'Sân bóng Mỹ Đình Arena','Sân 5 người gần Bến xe Mỹ Đình, mặt cỏ mới.','45 Lê Đức Thọ','Nam Từ Liêm','Hà Nội','https://picsum.photos/seed/snm3/800/500',350000,'["Cỏ nhân tạo","Đèn LED","Wifi"]','ACTIVE',4.6),
 (4,102,1,'Sân bóng Long Biên','Sân 7 người ven sông, thoáng mát.','88 Ngọc Lâm','Long Biên','Hà Nội','https://picsum.photos/seed/snm4/800/500',450000,'["Cỏ nhân tạo","Bãi giữ xe","Phòng thay đồ"]','ACTIVE',4.3),
 (5,101,2,'Cầu lông Thanh Xuân','4 sân thảm tiêu chuẩn, sàn gỗ, điều hòa.','102 Nguyễn Trãi','Thanh Xuân','Hà Nội','https://picsum.photos/seed/snm5/800/500',90000,'["Thảm tiêu chuẩn","Điều hòa","Cho thuê vợt"]','ACTIVE',4.8),
 (6,101,2,'Cầu lông Hoàng Mai','6 sân trong nhà, ánh sáng chống chói.','25 Giải Phóng','Hoàng Mai','Hà Nội','https://picsum.photos/seed/snm6/800/500',80000,'["Trong nhà","Đèn chống chói","Bãi giữ xe"]','ACTIVE',4.4),
 (7,102,2,'Cầu lông Tây Hồ','Sân cao cấp view hồ, có huấn luyện viên.','7 Xuân Diệu','Tây Hồ','Hà Nội','https://picsum.photos/seed/snm7/800/500',120000,'["Điều hòa","Huấn luyện viên","Phòng thay đồ","Nước uống"]','ACTIVE',4.9),
 (8,102,3,'Tennis Ba Đình','2 sân đất nện, có đèn buổi tối.','15 Hoàng Hoa Thám','Ba Đình','Hà Nội','https://picsum.photos/seed/snm8/800/500',250000,'["Sân đất nện","Đèn chiếu sáng","Cho thuê vợt"]','ACTIVE',4.6),
 (9,101,3,'Tennis Cầu Giấy Club','3 sân cứng tiêu chuẩn thi đấu.','30 Dương Đình Nghệ','Cầu Giấy','Hà Nội','https://picsum.photos/seed/snm9/800/500',280000,'["Sân cứng","Huấn luyện viên","Bãi giữ xe"]','ACTIVE',4.5),
 (10,101,4,'Bóng rổ Đống Đa','Sân bóng rổ ngoài trời, mặt sân sơn mới.','60 Tây Sơn','Đống Đa','Hà Nội','https://picsum.photos/seed/snm10/800/500',200000,'["Đèn LED","Bãi giữ xe","Nước uống"]','ACTIVE',4.2),
 (11,102,4,'Bóng rổ Nam Từ Liêm','Sân trong nhà, sàn gỗ, có khán đài.','9 Mễ Trì','Nam Từ Liêm','Hà Nội','https://picsum.photos/seed/snm11/800/500',260000,'["Trong nhà","Sàn gỗ","Khán đài","Điều hòa"]','ACTIVE',4.7),
 (12,102,2,'Cầu lông Đống Đa','Sân phong trào giá rẻ cho sinh viên.','18 Chùa Bộc','Đống Đa','Hà Nội','https://picsum.photos/seed/snm12/800/500',70000,'["Trong nhà","Quạt mát","Cho thuê vợt"]','ACTIVE',4.1);

-- Lịch hoạt động 06:00-22:00 cho cả 7 ngày (day_of_week: 1=Thứ 2 ... 7=Chủ nhật)
DELETE FROM venue_schedules WHERE venue_id BETWEEN 1 AND 12;
INSERT INTO venue_schedules(venue_id,day_of_week,open_time,close_time,slot_minutes,is_closed)
SELECT v.id, d.n, '06:00:00', '22:00:00', 60, FALSE
FROM venues v JOIN (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) d
WHERE v.id BETWEEN 1 AND 12;
