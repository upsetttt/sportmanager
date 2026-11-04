# Sân Nhà Mình

Website đặt lịch sân thể thao với giao diện React, Bootstrap và REST API Spring Boot, dùng mô hình Service/DAO với JDBC và MySQL.

## Yêu cầu

- Node.js 20+
- Java 21+
- Maven Wrapper tải và sử dụng Maven 3.9.15 tự động
- MySQL 8+

## Chạy ứng dụng

1. Cài đặt và khởi động MySQL 8+. Tạo database `sannhaminh`, hoặc để URL mặc định tự tạo database. Các thông tin kết nối, JWT và tài khoản chủ sân khởi tạo được cấu hình trong `.env` ở thư mục gốc. File này nằm trong `.gitignore`; không chia sẻ hoặc đưa thông tin bí mật lên Git.
2. Mở terminal tại thư mục gốc dự án và chạy backend bằng Maven Wrapper (không cần cài Maven toàn máy):

   ```powershell
   .\backend\mvnw.cmd -f .\backend\pom.xml spring-boot:run
   ```

   Đợi đến khi log báo `Tomcat started on port 8080` rồi giữ terminal này mở. Kiểm tra API tại `http://localhost:8080/api/venues`; kết quả HTTP 200 xác nhận backend đã sẵn sàng.

   Backend khởi tạo các bảng còn thiếu bằng `schema.sql`; tầng DAO dùng `JdbcTemplate`/JDBC và MySQL Connector/J, nghiệp vụ tài khoản, sân và đặt lịch được tách qua service interface và implementation. Có thể tạo cơ sở dữ liệu/bảng trước bằng script [database/sannhaminh.sql](./database/sannhaminh.sql).

   Thông tin tài khoản chủ sân mẫu:

   - Email: `owner@sannhaminh.local`
   - Mật khẩu: `SanNhaMinh@2026`

   Tài khoản được tạo tự động khi backend khởi động lần đầu theo `OWNER_EMAIL` và `OWNER_PASSWORD` trong `.env`. Nếu tài khoản đã tồn tại trong database, mật khẩu hiện có không bị ghi đè. Với database cũ, các sân mẫu chưa có chủ sở hữu sẽ được gán cho tài khoản này. Nếu đã đổi thông tin trong `.env` hoặc mật khẩu trong database, hãy đăng nhập bằng thông tin hiện tại của tài khoản.

   Nếu kết nối MySQL thất bại, kiểm tra `MYSQL_URL`, `MYSQL_USER` và `MYSQL_PASSWORD` trong `.env`. `JWT_SECRET` là bắt buộc và phải dài tối thiểu 32 byte. Trước khi triển khai, hãy thay mật khẩu mẫu và JWT secret bằng thông tin bí mật riêng.

3. Mở terminal thứ hai tại thư mục gốc và chạy frontend:

   ```powershell
   npm.cmd install
   npm.cmd run dev
   ```

   Dùng `npm.cmd` trong PowerShell để tránh lỗi Execution Policy khi chạy `npm.ps1`. Giữ terminal này mở và mở địa chỉ Vite được in ra, thường là `http://localhost:5173`.

4. Đăng nhập bằng tài khoản chủ sân mẫu ở trên. Nếu chọn mở giao diện tại `http://127.0.0.1:5173`, backend cũng cho phép origin này qua CORS; địa chỉ `http://localhost:5173` được hỗ trợ tương tự. Không dùng địa chỉ khác nếu chưa thêm địa chỉ đó vào danh sách origin trong `backend/src/main/java/vn/sannhaminh/booking/config/WebConfig.java`.

Nếu giao diện báo lỗi kết nối hoặc `ERR_CONNECTION_REFUSED`, kiểm tra cả hai terminal: backend phải chạy ở cổng `8080`, frontend ở cổng `5173`, và MySQL phải đang chạy. Lỗi CORS thường do frontend được mở bằng một địa chỉ hoặc cổng khác với origin đã cho phép.

## Kiểm thử

Chạy kiểm thử tích hợp backend với H2:

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml clean verify
```

Hiện có các test kiểm tra validation đăng ký, tạo/sửa/ẩn sân và quyền chủ sở hữu, đặt/hủy lịch, ngăn đặt trùng, giữ lịch sử sau khi ẩn sân và phân tách dữ liệu giữa tài khoản.

Maven Wrapper được ghim ở phiên bản 3.9.15 trong `backend/.mvn/wrapper/maven-wrapper.properties`; lần chạy đầu cần có kết nối Internet để tải Maven. Có thể chạy lệnh Wrapper ở trên từ terminal VS Code hoặc PowerShell. Nếu VS Code không tìm thấy task, chạy trực tiếp lệnh Wrapper thay vì phụ thuộc vào task. Có thể chọn **Terminal → Run Task → Backend: Tests** để kiểm thử khi task có sẵn.

## Phân tích và thiết kế

- Đặc tả Use Case (luồng chính và ngoại lệ): [docs/use-cases.md](./docs/use-cases.md)
- Biểu đồ Use Case tổng quát: [docs/uml/use-case-overview.puml](./docs/uml/use-case-overview.puml)
- Sequence đặt lịch: [docs/uml/booking-sequence.puml](./docs/uml/booking-sequence.puml)
- Sequence quản lý sân: [docs/uml/venue-management-sequence.puml](./docs/uml/venue-management-sequence.puml)
- SQL tạo database và cấu trúc bảng: [database/sannhaminh.sql](./database/sannhaminh.sql)

Backend dùng `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` và `PORT` để cấu hình môi trường; cổng mặc định là `8080`. Frontend mặc định gọi API tại `http://localhost:8080/api`; có thể thay bằng biến `VITE_API_URL`. CORS cho phép frontend từ `http://localhost:5173` và `http://127.0.0.1:5173`.

`JWT_SECRET` là bắt buộc, phải là chuỗi bí mật riêng, ngẫu nhiên và dài tối thiểu 32 byte; ứng dụng sẽ không khởi động nếu chưa cấu hình.

## REST API

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Đăng ký tài khoản khách hàng (`PLAYER`) |
| `POST` | `/api/auth/login` | Đăng nhập và nhận JWT có hiệu lực 12 giờ |
| `GET`, `PUT` | `/api/auth/me` | Xem hoặc cập nhật hồ sơ của tài khoản hiện tại |
| `GET` | `/api/venues` | Danh sách sân |
| `GET` | `/api/venues/{id}/availability?date=YYYY-MM-DD` | Các khung giờ đã được đặt |
| `GET` | `/api/venues/mine` | Danh sách sân thuộc tài khoản chủ sân |
| `POST`, `PUT`, `DELETE` | `/api/venues`, `/api/venues/{id}` | Thêm, sửa hoặc ẩn sân của chính chủ sở hữu |
| `POST` | `/api/bookings` | Tạo lịch đặt đã đăng nhập; thời lượng từ 1 đến 4 giờ |
| `GET` | `/api/bookings` | Tra cứu lịch theo tài khoản đang đăng nhập |
| `DELETE` | `/api/bookings/{id}` | Hủy lịch của tài khoản đang đăng nhập, trước giờ bắt đầu |
| `GET` | `/api/bookings/managed` | Chủ sân xem lịch đặt tại sân của mình |

Gửi JWT trong header `Authorization: Bearer <token>` cho API cần đăng nhập. Đăng ký công khai luôn tạo tài khoản khách hàng; vai trò chủ sân chỉ được cấp cho tài khoản khởi tạo trong cấu hình. Khách hàng quản lý hồ sơ và lịch đặt của mình; chủ sân chỉ sửa/ẩn các sân do chính tài khoản đó quản lý. Xóa sân là thao tác ẩn, giữ nguyên lịch sử đặt. Lịch đặt có khung giờ chồng lấn trên cùng sân sẽ bị từ chối. Dữ liệu sân mẫu được tự động thêm khi database chưa có sân.

## Ghi chú triển khai

Chủ sân mẫu dùng mật khẩu phát triển được cấu hình trong `.env`; phải đổi mật khẩu và JWT secret trước khi triển khai. Cổng thanh toán, xác minh email, khôi phục mật khẩu và quyền quản trị tập trung chưa được tích hợp. Cần chọn nhà cung cấp thanh toán và cấu hình thông tin xác thực trước khi bổ sung giao dịch thật. `schema.sql` chỉ khởi tạo các bảng còn thiếu; dùng migration được quản lý (Flyway/Liquibase) nếu cần thay đổi schema ở production.
