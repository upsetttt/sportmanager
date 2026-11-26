# Hoàn thiện dự án theo yêu cầu TX2

## Những phần đã cập nhật

1. **Tìm kiếm, lọc và phân trang**
   - Giữ nguyên `GET /api/venues` trả về mảng để không phá vỡ các màn hình cũ.
   - Bổ sung `GET /api/venues/page?page=0&size=9&q=...&categoryId=...&district=...&sort=...`.
   - `size` giới hạn 1–50; `page` không được âm; trường sắp xếp dùng whitelist, không ghép trực tiếp dữ liệu đầu vào vào SQL.
   - Màn hình React “Tìm sân” đã gọi API phân trang và có nút Trang trước/Trang sau.

2. **Bảo mật và phân quyền**
   - Loại bỏ bộ khung Spring Boot cũ nằm trong package `vn.sannhaminh.booking` vì nó khai báo thêm Application, SecurityConfig và các endpoint trùng với bộ API đang được React sử dụng.
   - Duy trì Spring Security + JWT; `@EnableMethodSecurity` và `@PreAuthorize` bảo vệ các API ADMIN/OWNER/PLAYER.
   - Request chưa đăng nhập trả HTTP 401; tài khoản không đủ quyền trả HTTP 403.
   - Mật khẩu dùng BCrypt; JWT secret phải được đổi trong `.env` trước khi triển khai.

3. **Validation và xử lý lỗi**
   - Bổ sung `ApiExceptionHandler` để trả JSON thống nhất cho lỗi validation, JSON sai định dạng và `ResponseStatusException`.
   - Các request DTO đăng ký/đăng nhập tiếp tục dùng Jakarta Validation.

4. **Tối ưu kết nối CSDL**
   - Cấu hình rõ HikariCP trong `application.yml`: pool name, maximum/minimum idle, connection timeout, idle timeout và max lifetime.
   - Có thể điều chỉnh `DB_POOL_MAX_SIZE` và `DB_POOL_MIN_IDLE` qua biến môi trường.

5. **Kiểm thử**
   - Bổ sung 8 bài kiểm thử HTTP/API: health, danh mục, tìm kiếm/lọc sân, phân trang, dữ liệu phân trang không hợp lệ, truy cập Admin khi chưa đăng nhập, User truy cập Admin và Admin truy cập hợp lệ.

## Chạy dự án

### Backend

```bash
cd backend
# Windows
mvnw.cmd spring-boot:run
# macOS/Linux
./mvnw spring-boot:run
```

### Kiểm thử backend

```bash
cd backend
mvnw.cmd test   # Windows
./mvnw test     # macOS/Linux
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Mở `http://localhost:5173`. Backend chạy tại `http://localhost:8080`.

## Lưu ý khi nghiệm thu

- Dự án hiện dùng JWT Bearer Token theo kiến trúc SPA React + REST API; đăng xuất ở giao diện xóa token khỏi trình duyệt. Đây là phiên xác thực stateless, không phải `HttpSession` phía máy chủ.
- Các test được bổ sung nhưng cần chạy trên máy có thể tải Maven dependencies. Trong môi trường chỉnh sửa này, Maven Wrapper không tải được Maven distribution do hạn chế kết nối mạng; frontend dependencies cũng chưa được cài nên chưa xác nhận được production build.
- Đề TX2 yêu cầu demo tối thiểu 5 kịch bản. Có thể dùng 8 test API ở trên kết hợp checklist nghiệp vụ tại `docs/TEST_CHECKLIST.md` để lập báo cáo kiểm thử.
