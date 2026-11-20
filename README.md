# Sân Nhà Mình – Nền tảng đặt sân thể thao

Dự án mới được xây dựng từ đầu theo mô hình frontend/backend tách biệt, dùng đúng môi trường phát triển đã thống nhất: React + Vite, Java 21 + Spring Boot 3, MySQL 8, IntelliJ IDEA và VS Code.

## 1. Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Frontend | React 18, Vite 6, JavaScript, CSS responsive, Axios, Lucide |
| Backend | Java 21, Spring Boot 3.5, Spring Security, JWT, JDBC Template |
| Database | MySQL 8+, InnoDB, UTF-8MB4 |
| API | REST/JSON, Bearer JWT |
| IDE | IntelliJ IDEA (backend), VS Code (frontend) |

## 2. Cấu trúc thư mục

```text
SanNhaMinh_New/
├── backend/
│   ├── pom.xml
│   ├── .env.example
│   └── src/main/
│       ├── java/vn/sannhaminh/
│       │   ├── auth/                 # Đăng ký, đăng nhập
│       │   ├── api/                  # API nghiệp vụ và phân quyền
│       │   ├── config/               # Security, khởi tạo Admin
│       │   └── security/             # JWT, principal, filter
│       └── resources/
│           ├── application.yml
│           └── schema.sql
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── services/api.js
│       ├── App.jsx
│       └── styles.css
├── database/sannhaminh.sql
└── docs/
```

## 3. Chuẩn bị MySQL Workbench

1. Mở MySQL Workbench và kết nối MySQL Server 8.
2. Mở file `database/sannhaminh.sql` bằng **File → Open SQL Script**.
3. Nhấn nút tia sét **Execute** để chạy toàn bộ script. Script tự tạo và chọn database `sannhaminh_new`, sau đó tạo các bảng và danh mục mẫu.
4. Trong mục **SCHEMAS**, nhấn Refresh và kiểm tra database `sannhaminh_new` cùng các bảng `accounts`, `categories`, `venues`, `bookings`, `payments`, `reviews`, `favorites`, `notifications`, `audit_logs`, `venue_schedules`.
5. Nếu đang chuyển dữ liệu cũ, sao lưu trước và làm theo `docs/MIGRATION_FROM_OLD_VERSION.md`; không xóa database cũ khi chưa sao lưu.

## 4. Cấu hình và chạy backend bằng VS Code / IntelliJ

1. Mở thư mục `SanNhaMinh_New/backend`.
2. Sao chép `backend/.env.example` thành `backend/.env`.
3. Mở `backend/.env` và thay `DB_PASSWORD=YOUR_MYSQL_ROOT_PASSWORD` bằng mật khẩu MySQL thật. Nếu tài khoản MySQL không phải `root`, sửa `DB_USERNAME` tương ứng. Không đưa file `.env` lên Git.
4. `application.yml` đã nạp file `.env` cục bộ (khi chạy với working directory là thư mục `backend`) và kết nối mặc định tới database `sannhaminh_new` trên cổng 3306.
5. Mở Terminal tại thư mục `backend` và chạy:

```powershell
.\mvnw.cmd spring-boot:run
```

Nếu dự án không có `mvnw.cmd`, dùng `mvn spring-boot:run` khi đã cài Maven.

6. Kiểm tra `http://localhost:8080/api/health`; kết quả mong đợi có `status: UP`.

Tài khoản Admin mặc định chỉ được tạo khi database chưa có tài khoản ADMIN. Thông tin được lấy từ `ADMIN_EMAIL` và `ADMIN_PASSWORD` trong `backend/.env`; hãy đổi mật khẩu trước lần chạy đầu tiên. Nếu đã có ADMIN trong database, ứng dụng không tự ghi đè mật khẩu hiện tại.

## 5. Chạy frontend bằng VS Code

1. Mở VS Code, chọn **File → Open Folder**, mở `SanNhaMinh_New/frontend`.
2. Mở Terminal → New Terminal.
3. Cài thư viện:

```powershell
npm install
```

4. Chạy ứng dụng:

```powershell
npm run dev
```

5. Truy cập `http://localhost:5173`.

Vite đã cấu hình proxy `/api` tới `http://localhost:8080`, vì vậy không cần tự sửa URL API khi chạy local.

## 6. Vai trò và chức năng

### PLAYER – Khách hàng
- Đăng ký, đăng nhập với vai trò khách hàng.
- Tìm kiếm sân theo tên, địa chỉ, khu vực và bộ môn.
- Xem thông tin sân, giá thuê, đánh giá.
- Gửi yêu cầu đặt sân theo ngày, giờ và thời lượng.
- Theo dõi lịch đặt; hủy lịch theo trạng thái cho phép.
- Lưu/bỏ lưu sân yêu thích.
- Chọn chuyển khoản hoặc tiền mặt; xem trạng thái thanh toán.
- Đánh giá sân sau khi lịch đặt hoàn thành.
- Nhận và đánh dấu thông báo đã đọc.

### OWNER – Chủ sân
- Đăng ký tài khoản chủ sân.
- Thêm, sửa, lưu trữ sân; sân mới chờ Admin duyệt.
- Cấu hình giờ mở cửa, giờ đóng cửa, ngày nghỉ và bước thời lượng.
- Xem yêu cầu đặt sân; xác nhận hoặc từ chối.
- Đánh dấu lịch đã xác nhận là hoàn thành.
- Theo dõi doanh thu và lịch đặt.
- Xem đánh giá, phản hồi khách hàng.

### ADMIN – Quản trị viên
- Dashboard thống kê tài khoản, sân, booking, thanh toán và doanh thu.
- Thêm/sửa tài khoản; khóa, mở khóa, vô hiệu hóa và đặt lại mật khẩu.
- Thêm/sửa sân, duyệt/từ chối, lưu trữ và khôi phục sân.
- Theo dõi và hủy booking khi cần.
- Xác nhận thanh toán thủ công; xác nhận hoàn tiền sau khi đã hoàn tiền thực tế ngoài hệ thống.
- Ẩn/hiện đánh giá.
- Thêm/sửa/vô hiệu hóa danh mục.
- Gửi thông báo theo nhóm người dùng.
- Theo dõi nhật ký thao tác quản trị và doanh thu.

## 7. Quy tắc nghiệp vụ quan trọng

- Booking mới có trạng thái `PENDING_OWNER`; chỉ chủ sân mới xác nhận hoặc từ chối.
- Hệ thống chặn đặt trùng khung giờ đối với booking `PENDING_OWNER` và `CONFIRMED`.
- Nếu chủ sân đã cấu hình giờ hoạt động, thời gian đặt phải nằm trong khung giờ mở cửa.
- Chỉ booking `CONFIRMED` mới được tạo thông tin thanh toán.
- Chuyển khoản/tiền mặt tạo giao dịch `PENDING`; Admin xác nhận thủ công mới chuyển sang `PAID`.
- Khi hủy booking đã thanh toán, payment chuyển `REFUND_PENDING`. Admin chỉ đánh dấu `REFUNDED` sau khi tiền đã được hoàn thực tế bên ngoài hệ thống.
- Tài khoản/sân/danh mục được vô hiệu hóa hoặc lưu trữ mềm; không xóa lịch sử booking và thanh toán.
- VNPay/MoMo đang tắt. API từ chối tạo giao dịch online cho đến khi tích hợp merchant, chữ ký và callback/IPN thật.

## 8. Bảo mật và triển khai

- Mật khẩu được băm bằng BCrypt.
- API bảo vệ bằng JWT Bearer Token và phân quyền PLAYER/OWNER/ADMIN.
- Tài khoản bị khóa/vô hiệu hóa không đăng nhập được.
- Các thao tác quan trọng của Admin được ghi vào `audit_logs`.
- Trước khi triển khai internet: thay JWT secret, mật khẩu Admin, cấu hình HTTPS, CORS domain thật, giới hạn request, backup MySQL và bổ sung xác minh email/khôi phục mật khẩu.

## 9. Kiểm thử

Xem `docs/TEST_CHECKLIST.md` để kiểm tra lần lượt đăng ký, đăng nhập, quyền truy cập, đặt sân, xử lý booking, thanh toán, hoàn tiền và thao tác Admin.

> Trạng thái bản bàn giao: mã nguồn và cấu trúc dự án đã được tạo. Trong môi trường đóng gói hiện tại không có Maven executable và npm install bị timeout khi tải dependency, do đó chưa thể xác nhận Maven test hoặc Vite production build. Cần cài dependency trên máy phát triển và chạy checklist trước khi dùng thật.

## 10. Xử lý lỗi kết nối và lưu sân

- `Access denied for user`: kiểm tra `DB_USERNAME` và `DB_PASSWORD` trong `backend/.env`.
- `Unknown database 'sannhaminh_new'`: chạy `database/sannhaminh.sql` trong MySQL Workbench và kiểm tra schema đã chọn.
- `Communications link failure`: khởi động MySQL Server và kiểm tra cổng 3306.
- Thêm sân bằng tài khoản OWNER gọi `POST /api/owner/venues`; tài khoản ADMIN dùng `POST /api/admin/venues` và cần `ownerId`.
- Nếu API trả lỗi, giao diện giữ nguyên biểu mẫu và hiển thị mã HTTP cùng thông báo backend; mở trình duyệt Developer Tools → Network để xem request/response.
