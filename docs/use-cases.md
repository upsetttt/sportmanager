# Đặc tả Use Case — Sân Nhà Mình

## UC-01: Đăng ký và đăng nhập

| Thuộc tính | Mô tả |
| --- | --- |
| Tác nhân chính | Khách hàng; Chủ sân đã được cấp tài khoản |
| Mục tiêu | Tạo phiên đăng nhập để sử dụng các chức năng theo vai trò |
| Tiền điều kiện | Dịch vụ đang hoạt động; email chưa được đăng ký khi tạo tài khoản |
| Kích hoạt | Người dùng chọn Đăng ký hoặc Đăng nhập |
| Hậu điều kiện | Tài khoản được lưu an toàn; phiên đăng nhập nhận JWT có hạn 12 giờ |

**Luồng chính**

1. Người dùng mở form đăng nhập hoặc chuyển sang form đăng ký.
2. Khi đăng ký, người dùng nhập họ tên, email, số điện thoại và mật khẩu; form kiểm tra trường bắt buộc và định dạng cơ bản.
3. Hệ thống kiểm tra dữ liệu ở backend, chuẩn hóa email, mã hóa mật khẩu và tạo tài khoản vai trò khách hàng.
4. Khi đăng nhập, hệ thống xác thực thông tin với mật khẩu đã mã hóa.
5. Hệ thống cấp JWT và hiển thị giao diện phù hợp với vai trò.

**Ngoại lệ**

- Email sai định dạng, số điện thoại sai định dạng, thiếu trường hoặc mật khẩu ngắn hơn 8 ký tự: báo lỗi validation và không ghi dữ liệu.
- Email đã tồn tại: từ chối đăng ký và thông báo người dùng.
- Thông tin đăng nhập không hợp lệ hoặc tài khoản không còn tồn tại: từ chối xác thực.
- Lỗi kết nối cơ sở dữ liệu: không tạo phiên thành công; trả lỗi rõ ràng để người dùng thử lại.

## UC-02: Tìm sân và đặt lịch

| Thuộc tính | Mô tả |
| --- | --- |
| Tác nhân chính | Khách hàng đã đăng nhập |
| Mục tiêu | Tìm sân phù hợp và đặt một khung giờ còn trống |
| Tiền điều kiện | Có sân đang hoạt động; người dùng có tài khoản khách hàng |
| Kích hoạt | Người dùng chọn Đặt sân trên thẻ sân |
| Hậu điều kiện | Lịch đặt được lưu với trạng thái đã xác nhận; giờ đó không thể bị đặt chồng |

**Luồng chính**

1. Người dùng tìm kiếm theo tên/khu vực, môn thể thao, sân yêu thích hoặc tiêu chí sắp xếp.
2. Hệ thống hiển thị sân đang hoạt động, thông tin giá, địa chỉ và tiện ích.
3. Người dùng chọn sân, ngày, giờ bắt đầu và thời lượng từ 1 đến 4 giờ.
4. Hệ thống tải các lịch đã xác nhận trong ngày và vô hiệu hóa khung giờ đã qua/đã kín.
5. Người dùng xác nhận; backend khóa bản ghi sân, xác thực lại dữ liệu và kiểm tra chồng lấn trong giao dịch.
6. Hệ thống lưu lịch, tính tổng giá theo thời lượng, rồi trả mã đặt sân.

**Ngoại lệ**

- Chưa đăng nhập: chuyển đến form xác thực, sau đó tiếp tục quy trình đặt sân.
- Sân không tồn tại hoặc đã bị ẩn: từ chối đặt.
- Ngày/giờ đã qua, thời lượng không hợp lệ hoặc giờ vừa được người khác đặt: từ chối yêu cầu và yêu cầu chọn lại.
- Không tải được tình trạng giờ trống hoặc máy chủ lỗi: không cho gửi đặt lịch và hiển thị thông báo.

## UC-03: Chủ sân quản lý sân và theo dõi lịch

| Thuộc tính | Mô tả |
| --- | --- |
| Tác nhân chính | Chủ sân |
| Mục tiêu | Tạo/cập nhật/ẩn sân do mình sở hữu và xem lịch đặt tại các sân đó |
| Tiền điều kiện | Đã đăng nhập bằng tài khoản có vai trò chủ sân |
| Kích hoạt | Chủ sân mở mục Quản lý sân |
| Hậu điều kiện | Thay đổi sân được lưu; lịch sử đặt không bị xóa khi sân bị ẩn |

**Luồng chính**

1. Chủ sân mở dashboard; hệ thống tải danh sách sân thuộc tài khoản và lịch đặt liên quan.
2. Chủ sân nhập tên, môn, địa chỉ, giá, ảnh và tiện ích; form kiểm tra dữ liệu bắt buộc, kiểu URL và giá dương.
3. Chủ sân chọn đăng mới hoặc lưu chỉnh sửa; backend kiểm tra quyền sở hữu và validation.
4. Hệ thống lưu thay đổi và làm mới dashboard/danh sách sân công khai.
5. Khi cần ngừng nhận lịch, chủ sân chọn Ẩn; hệ thống đánh dấu sân không hoạt động, giữ nguyên lịch sử đặt.

**Ngoại lệ**

- Tài khoản khách hàng hoặc người chưa đăng nhập: từ chối truy cập dashboard.
- Dữ liệu sân không hợp lệ: hiển thị lỗi và giữ nguyên dữ liệu đã nhập.
- Sân không tồn tại hoặc thuộc chủ sở hữu khác: không tiết lộ dữ liệu và từ chối cập nhật/ẩn.
- Lỗi tải/lưu dữ liệu: hiển thị lỗi, không báo thành công giả.
