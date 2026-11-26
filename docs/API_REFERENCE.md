# API Reference – Sân Nhà Mình

Base URL local: `http://localhost:8080/api`. Các endpoint cần đăng nhập nhận header `Authorization: Bearer <JWT>`.

## Authentication
| Method | Endpoint | Quyền | Mô tả |
|---|---|---|---|
| POST | `/auth/register` | Public | Tạo PLAYER/OWNER |
| POST | `/auth/login` | Public | Đăng nhập, trả JWT |
| GET | `/health` | Public | Health check |

## Public discovery
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/categories` | Danh mục đang hoạt động |
| GET | `/venues?q=&categoryId=&district=&sort=` | Tìm sân đang hoạt động |
| GET | `/venues/{id}` | Chi tiết sân và đánh giá hiển thị |

## PLAYER
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/player/dashboard` | Thống kê và lịch sắp tới |
| POST | `/bookings` | Gửi yêu cầu đặt sân |
| GET | `/bookings/mine` | Lịch đặt của tôi |
| PATCH | `/bookings/{id}/cancel` | Hủy lịch |
| POST | `/payments` | Gửi lựa chọn thanh toán CASH/BANK_TRANSFER |
| GET | `/payments/mine` | Lịch sử thanh toán |
| POST | `/favorites/{venueId}` | Bật/tắt yêu thích |
| GET | `/favorites` | Danh sách yêu thích |
| POST | `/reviews` | Đánh giá booking COMPLETED |
| GET | `/reviews/mine` | Đánh giá của tôi |
| GET | `/notifications` | Thông báo của tôi |
| PATCH | `/notifications/{id}/read` | Đánh dấu đã đọc |

### POST `/bookings`
```json
{
  "venueId": 1,
  "playDate": "2026-12-01",
  "startTime": "18:00",
  "hours": 2,
  "note": "Cho thuê bóng nếu có"
}
```

## OWNER
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/owner/dashboard` | Thống kê chủ sân |
| GET/POST | `/owner/venues` | Danh sách / thêm sân |
| PUT/DELETE | `/owner/venues/{id}` | Sửa / lưu trữ mềm sân |
| GET/PUT | `/owner/venues/{id}/schedules` | Xem / lưu lịch hoạt động 7 ngày |
| GET | `/owner/bookings` | Lịch đặt tại sân của tôi |
| GET | `/owner/revenue` | Giao dịch đã thanh toán thuộc sân của tôi |
| PATCH | `/owner/bookings/{id}/decision` | Xác nhận hoặc từ chối |
| PATCH | `/owner/bookings/{id}/complete` | Đánh dấu hoàn thành |
| GET | `/owner/reviews` | Đánh giá tại sân của tôi |
| PATCH | `/owner/reviews/{id}/reply` | Phản hồi đánh giá |

### PATCH `/owner/bookings/{id}/decision`
```json
{ "accept": true, "note": "Đã xác nhận sân" }
```

## ADMIN
| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/admin/dashboard` | Thống kê hệ thống |
| GET/POST | `/admin/accounts` | Danh sách / tạo tài khoản |
| PUT | `/admin/accounts/{id}` | Sửa tài khoản |
| PATCH | `/admin/accounts/{id}/status` | ACTIVE/LOCKED/DELETED |
| PATCH | `/admin/accounts/{id}/password` | Đặt lại mật khẩu |
| GET/POST | `/admin/venues` | Danh sách / tạo sân |
| PUT | `/admin/venues/{id}` | Sửa sân |
| PATCH | `/admin/venues/{id}/status` | ACTIVE/REJECTED/ARCHIVED |
| GET | `/admin/bookings` | Danh sách booking |
| PATCH | `/admin/bookings/{id}/cancel` | Hủy booking |
| GET | `/admin/payments` | Danh sách giao dịch |
| PATCH | `/admin/payments/{id}/confirm` | Xác nhận nhận tiền |
| PATCH | `/admin/payments/{id}/refund` | Xác nhận hoàn tiền thực tế |
| GET | `/admin/reviews` | Danh sách đánh giá |
| PATCH | `/admin/reviews/{id}/visibility` | Ẩn/hiện đánh giá |
| GET/POST | `/admin/categories` | Danh sách / thêm danh mục |
| PUT/DELETE | `/admin/categories/{id}` | Sửa / vô hiệu hóa danh mục |
| POST | `/admin/notifications` | Gửi thông báo theo nhóm |
| GET | `/admin/revenue?period=day\|month\|year` | Tổng hợp doanh thu đã thanh toán |
| GET | `/admin/audit-logs` | Nhật ký quản trị |

## Payment state machine
- `PENDING` → `PAID` (Admin xác nhận giao dịch chuyển khoản/tiền mặt).
- `PAID` → `REFUND_PENDING` (booking đã thanh toán bị hủy).
- `REFUND_PENDING` → `REFUNDED` (Admin xác nhận tiền đã hoàn thực tế).
- `PENDING` → `CANCELLED` (booking bị hủy trước khi xác nhận thanh toán).

VNPay/MoMo chưa có endpoint tạo giao dịch hoạt động; cần bổ sung adapter ký request, kiểm tra chữ ký callback/IPN và idempotency trước khi bật.
