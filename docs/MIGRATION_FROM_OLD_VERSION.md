# Chuyển dữ liệu từ Sân Nhà Mình phiên bản cũ

Bản mới sử dụng schema `sannhaminh_new` độc lập, không ghi đè database cũ. Nếu muốn giữ dữ liệu của phiên bản trước, thực hiện theo quy trình dưới đây.

1. Backup toàn bộ schema cũ `sannhaminh` bằng MySQL Workbench → Data Export.
2. Tạo schema `sannhaminh_new` trong MySQL Workbench.
3. Mở `database/sannhaminh.sql`, chọn schema `sannhaminh_new` làm default schema và chạy toàn bộ script để tạo bảng mới. Chưa khởi động backend ở bước này để tránh tạo Admin mặc định gây trùng ID với tài khoản cũ.
4. Mở `database/migrate_legacy_sannhaminh.sql`, kiểm tra tên schema nguồn. Nếu schema cũ không tên `sannhaminh`, thay tiền tố schema tương ứng.
5. Chạy script migration và xem bảng tổng kết số lượng bản ghi.
6. Đối chiếu số lượng accounts, venues, bookings, payments, reviews và favorites giữa hai phiên bản.
7. Khởi động backend mới. Nếu dữ liệu cũ đã có Admin thì giữ tài khoản đó; nếu chưa có Admin, backend tự tạo tài khoản từ `ADMIN_EMAIL` và `ADMIN_PASSWORD`.
8. Không xóa schema cũ cho đến khi kiểm thử đăng nhập, lịch sử đặt sân và thanh toán trên hệ thống mới.

## Các điểm cần biết

- Tài khoản cũ giữ nguyên ID, email và password hash. Trạng thái `account_access.enabled=false` được chuyển thành `LOCKED`.
- Khách từng đặt sân nhưng chưa có tài khoản sẽ được tạo hồ sơ `LOCKED` với password hash placeholder. Admin cần cấp lại mật khẩu trước khi họ đăng nhập.
- Sân giữ ID, giá, ảnh và rating; loại sân cũ `sport` được đưa vào bảng categories.
- Lịch hoạt động, booking, payment, review/reply, favorites và notifications được ánh xạ sang schema mới.
- Nếu sân cũ không có owner_id và không tồn tại tài khoản ADMIN trong dữ liệu mới, sân đó sẽ không được import do schema mới yêu cầu một chủ sở hữu hợp lệ.
- Script là additive và dùng `INSERT IGNORE`; không xóa dữ liệu nguồn. Hãy chạy trên bản backup trước, không chạy lần đầu trên production.
