# Checklist kiểm thử thủ công

## A. Khởi động
- [ ] MySQL service đang chạy, database `sannhaminh_new` đã được tạo.
- [ ] Backend khởi động không lỗi và `/api/health` trả `UP`.
- [ ] Frontend chạy ở `http://localhost:5173`.
- [ ] Đăng nhập được bằng tài khoản Admin khởi tạo; đổi mật khẩu mặc định.

## B. PLAYER
- [ ] Đăng ký PLAYER, đăng xuất, đăng nhập.
- [ ] Tìm sân theo từ khóa, quận/huyện, danh mục.
- [ ] Đặt sân vào khung giờ trống; booking có trạng thái `PENDING_OWNER`.
- [ ] Đặt trùng khung giờ đang chờ/đã xác nhận phải bị từ chối.
- [ ] Đặt ngoài lịch hoạt động của sân phải bị từ chối (nếu chủ sân đã cấu hình lịch).
- [ ] Hủy booking đang chờ/đã xác nhận.
- [ ] Thêm/bỏ sân yêu thích.
- [ ] Sau khi booking hoàn thành, gửi đánh giá.
- [ ] Đọc thông báo và xác nhận trạng thái đã đọc.

## C. OWNER
- [ ] Đăng ký OWNER.
- [ ] Thêm sân, kiểm tra sân ở trạng thái `PENDING`.
- [ ] Admin duyệt sân; sân xuất hiện trong kết quả tìm kiếm.
- [ ] Sửa thông tin sân, cấu hình lịch 7 ngày, đóng một ngày.
- [ ] Khách gửi booking; Owner thấy yêu cầu mới.
- [ ] Owner xác nhận booking; khách nhìn thấy `CONFIRMED`.
- [ ] Owner từ chối booking; khách nhìn thấy `REJECTED`.
- [ ] Đánh dấu booking hoàn thành và phản hồi đánh giá.

## D. ADMIN
- [ ] Tạo/sửa tài khoản; khóa/mở khóa; vô hiệu hóa; đặt lại mật khẩu.
- [ ] Thử đăng nhập tài khoản bị khóa/vô hiệu hóa: phải bị từ chối.
- [ ] Tạo/sửa sân, duyệt/từ chối, lưu trữ và khôi phục.
- [ ] Xem booking và hủy booking.
- [ ] Xác nhận payment PENDING.
- [ ] Hủy booking đã thanh toán: payment chuyển `REFUND_PENDING`.
- [ ] Chỉ sau khi hoàn tiền thực tế mới xác nhận `REFUNDED`.
- [ ] Ẩn/hiện review, quản lý danh mục, gửi thông báo.
- [ ] Kiểm tra nhật ký quản trị và doanh thu.

## E. Responsive và bảo mật
- [ ] Kiểm tra giao diện ở desktop, tablet và mobile.
- [ ] Không thể gọi API ADMIN bằng token PLAYER/OWNER.
- [ ] Không thể sửa sân/booking thuộc tài khoản khác.
- [ ] Thay JWT secret, mật khẩu Admin mặc định và cấu hình CORS domain trước khi deploy.
