# Chuẩn bị tích hợp VNPay / MoMo

Trong bản này, VNPay và MoMo chỉ xuất hiện như phương thức dự kiến. Không bật giao dịch thật khi chưa có thông tin merchant. Backend chủ động trả lỗi `501 NOT_IMPLEMENTED` nếu khách chọn hai phương thức này.

Khi có thông tin đối tác, cần triển khai riêng từng adapter:

1. Cấu hình merchant code/partner code, secret key, endpoint sandbox/production và return URL/IPN URL trong biến môi trường hoặc secret manager. Không commit khóa vào Git.
2. Tạo payment intent có mã đơn duy nhất và lưu trạng thái `PENDING` trước khi chuyển khách sang cổng thanh toán.
3. Ký request theo đúng tài liệu chính thức của nhà cung cấp.
4. Tạo callback/IPN endpoint public riêng, xác minh chữ ký, số tiền, mã đơn, trạng thái và chống xử lý lặp (idempotency).
5. Chỉ cập nhật `PAID` từ callback/IPN hợp lệ đã xác minh phía server; không tin query string hoặc trang return của trình duyệt.
6. Ghi request ID, transaction reference, response code và audit log; không ghi secret hoặc dữ liệu thẻ.
7. Kiểm thử sandbox: thành công, thất bại, timeout, callback lặp, sai chữ ký, sai số tiền, người dùng đóng trình duyệt.
8. Chỉ bật `VNPAY_ENABLED` / `MOMO_ENABLED` sau khi hoàn thành kiểm thử và đối soát.

Thông tin cần có để tích hợp: merchant/partner ID, secret key, tài liệu phiên bản API đang dùng, sandbox credentials, callback/IPN URL public HTTPS và tài khoản kiểm thử. Các thông tin này nên gửi qua kênh bảo mật, không đưa vào source code.
