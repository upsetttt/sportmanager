package vn.sannhaminh.api;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.sannhaminh.security.AuthUser;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final List<String> VALID_ROLES = List.of("PLAYER", "OWNER", "ADMIN");
    private static final List<String> VALID_ACCOUNT_STATUSES = List.of("ACTIVE", "LOCKED", "DELETED");
    private static final List<String> VALID_VENUE_STATUSES = List.of("ACTIVE", "REJECTED", "ARCHIVED");
    private static final List<String> VALID_PAYMENT_METHODS = List.of("BANK_TRANSFER", "CASH", "VNPAY", "MOMO");

    private final JdbcTemplate db;
    private final PasswordEncoder passwordEncoder;

    public ApiController(JdbcTemplate db, PasswordEncoder passwordEncoder) {
        this.db = db;
        this.passwordEncoder = passwordEncoder;
    }

    private long uid(AuthUser u) { return u.id(); }

    private void audit(AuthUser u, String action, String type, Long id, String detail) {
        db.update("INSERT INTO audit_logs(actor_id,action,target_type,target_id,details) VALUES(?,?,?,?,?)",
                u == null ? null : u.id(), action, type, id, detail);
    }

    private void notify(long account, String title, String message) {
        db.update("INSERT INTO notifications(account_id,title,message) VALUES(?,?,?)", account, title, message);
    }

    private Map<String, Object> body(Map<String, Object> b) { return b == null ? new HashMap<>() : b; }

    private String s(Map<String, Object> b, String k, String d) {
        Object v = b == null ? null : b.get(k);
        return v == null ? d : String.valueOf(v);
    }

    private long n(Map<String, Object> b, String k, long d) {
        try {
            Object v = b == null ? null : b.get(k);
            if (v == null) return d;
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return d;
        }
    }

    private long insertAndGetId(String sql, Object... args) {
        var keys = new GeneratedKeyHolder();
        db.update(con -> {
            var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keys);
        Number id = keys.getKey();
        if (id == null) {
            throw new IllegalStateException("Không thể lấy ID bản ghi mới tạo");
        }
        return id.longValue();
    }

    private String requireText(Map<String, Object> b, String key, String label, int maxLen) {
        String value = s(b, key, "").trim();
        if (value.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " không được để trống");
        }
        if (value.length() > maxLen) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " tối đa " + maxLen + " ký tự");
        }
        return value;
    }

    private String requireEmail(Map<String, Object> b) {
        String email = requireText(b, "email", "Email", 190).toLowerCase();
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@") || email.contains(" ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ");
        }
        return email;
    }

    private BigDecimal requirePositivePrice(Map<String, Object> b) {
        String raw = s(b, "pricePerHour", "").trim();
        if (raw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá thuê mỗi giờ không được để trống");
        }
        try {
            BigDecimal price = new BigDecimal(raw);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá thuê mỗi giờ phải lớn hơn 0");
            }
            return price;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá thuê mỗi giờ không hợp lệ");
        }
    }

    private Long parseOptionalCategoryId(Map<String, Object> b) {
        Object raw = b == null ? null : b.get("categoryId");
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            return null;
        }
        long categoryId;
        try {
            categoryId = Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục sân không hợp lệ");
        }
        Integer exists = db.queryForObject("SELECT COUNT(*) FROM categories WHERE id=? AND active=1", Integer.class, categoryId);
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục sân không tồn tại hoặc đã ngừng hoạt động");
        }
        return categoryId;
    }

    private long requireValidVenueOwner(Map<String, Object> b) {
        long ownerId = n(b, "ownerId", 0);
        if (ownerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID tài khoản chủ sân không hợp lệ");
        }
        var rows = db.queryForList("SELECT role, status FROM accounts WHERE id=?", ownerId);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy tài khoản chủ sân");
        }
        Map<String, Object> acc = rows.get(0);
        if (!"ACTIVE".equals(acc.get("status"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản chủ sân đang bị khóa hoặc vô hiệu hóa");
        }
        String role = String.valueOf(acc.get("role"));
        if (!"OWNER".equals(role) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản được chọn không có quyền chủ sân");
        }
        return ownerId;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "San Nha Minh API");
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return db.queryForList("SELECT * FROM categories WHERE active=1 ORDER BY name");
    }

    @GetMapping("/venues")
    public List<Map<String, Object>> venues(@RequestParam(name = "q", required = false) String q,
                                            @RequestParam(name = "categoryId", required = false) Long categoryId,
                                            @RequestParam(name = "district", required = false) String district,
                                            @RequestParam(name = "sort", required = false) String sort) {
        String sql = "SELECT v.*,a.full_name owner_name,c.name category_name FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id WHERE v.status='ACTIVE' AND a.status='ACTIVE'";
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            sql += " AND (v.name LIKE ? OR v.address LIKE ? OR v.district LIKE ?)";
            String x = "%" + q.trim() + "%";
            args.add(x); args.add(x); args.add(x);
        }
        if (categoryId != null) { sql += " AND v.category_id=?"; args.add(categoryId); }
        if (district != null && !district.isBlank()) { sql += " AND v.district LIKE ?"; args.add("%" + district.trim() + "%"); }
        String order = switch (sort == null ? "" : sort) {
            case "price_asc" -> "v.price_per_hour ASC";
            case "price_desc" -> "v.price_per_hour DESC";
            case "rating" -> "v.rating DESC";
            default -> "v.created_at DESC";
        };
        sql += " ORDER BY " + order;
        return db.queryForList(sql, args.toArray());
    }

    /** API phân trang riêng để không phá vỡ hợp đồng mảng JSON mà giao diện TX1 đang sử dụng. */
    @GetMapping("/venues/page")
    public Map<String, Object> venuesPage(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "district", required = false) String district,
            @RequestParam(name = "sort", defaultValue = "created_desc") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page phải lớn hơn hoặc bằng 0");
        if (size < 1 || size > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size phải từ 1 đến 50");
        String from = " FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id WHERE v.status='ACTIVE' AND a.status='ACTIVE'";
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            from += " AND (v.name LIKE ? OR v.address LIKE ? OR v.district LIKE ?)";
            String x = "%" + q.trim() + "%";
            args.add(x); args.add(x); args.add(x);
        }
        if (categoryId != null) { from += " AND v.category_id=?"; args.add(categoryId); }
        if (district != null && !district.isBlank()) { from += " AND v.district LIKE ?"; args.add("%" + district.trim() + "%"); }
        String order = switch (sort) {
            case "price_asc" -> "v.price_per_hour ASC";
            case "price_desc" -> "v.price_per_hour DESC";
            case "rating" -> "v.rating DESC";
            default -> "v.created_at DESC";
        };
        Integer total = db.queryForObject("SELECT COUNT(*)" + from, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<Map<String, Object>> content = db.queryForList(
                "SELECT v.*,a.full_name owner_name,c.name category_name" + from + " ORDER BY " + order + " LIMIT ? OFFSET ?",
                pageArgs.toArray());
        int totalElements = total == null ? 0 : total;
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        return Map.of("content", content, "page", page, "size", size, "totalElements", totalElements, "totalPages", totalPages,
                "first", page == 0, "last", page + 1 >= totalPages);
    }

    @GetMapping("/venues/{id}")
    public Map<String, Object> venue(@PathVariable("id") long id) {
        var rows = db.queryForList(
                "SELECT v.*,a.full_name owner_name,a.phone owner_phone,c.name category_name FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id WHERE v.id=? AND v.status='ACTIVE' AND a.status='ACTIVE'",
                id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân");
        Map<String, Object> result = rows.get(0);
        result.put("reviews", db.queryForList(
                "SELECT r.id,r.rating,r.content,r.owner_reply,r.created_at,a.full_name player_name FROM reviews r JOIN accounts a ON a.id=r.player_id WHERE r.venue_id=? AND r.status='VISIBLE' ORDER BY r.created_at DESC",
                id));
        return result;
    }

    @GetMapping("/player/dashboard")
    @PreAuthorize("hasRole('PLAYER')")
    public Map<String, Object> playerDashboard(@AuthenticationPrincipal AuthUser u) {
        return Map.of(
                "upcoming", db.queryForList(
                        "SELECT b.*,v.name venue_name,v.address FROM bookings b JOIN venues v ON v.id=b.venue_id WHERE b.player_id=? AND b.status IN ('PENDING_OWNER','CONFIRMED') AND b.play_date>=CURDATE() ORDER BY b.play_date,b.start_time LIMIT 6",
                        uid(u)),
                "bookingCount", db.queryForObject("SELECT COUNT(*) FROM bookings WHERE player_id=?", Integer.class, uid(u)),
                "favoriteCount", db.queryForObject(
                        "SELECT COUNT(*) FROM favorites f JOIN venues v ON v.id=f.venue_id JOIN accounts a ON a.id=v.owner_id WHERE f.player_id=? AND v.status='ACTIVE' AND a.status='ACTIVE'",
                        Integer.class, uid(u)));
    }

    @GetMapping("/owner/dashboard")
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> ownerDashboard(@AuthenticationPrincipal AuthUser u) {
        return Map.of(
                "venueCount", db.queryForObject("SELECT COUNT(*) FROM venues WHERE owner_id=? AND status<>'ARCHIVED'", Integer.class, uid(u)),
                "pendingCount", db.queryForObject(
                        "SELECT COUNT(*) FROM bookings b JOIN venues v ON v.id=b.venue_id WHERE (b.owner_id=? OR v.owner_id=?) AND b.status='PENDING_OWNER'",
                        Integer.class, uid(u), uid(u)),
                "monthRevenue", db.queryForObject(
                        "SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN venues v ON v.id=b.venue_id WHERE (b.owner_id=? OR v.owner_id=?) AND p.status='PAID' AND MONTH(p.created_at)=MONTH(CURDATE()) AND YEAR(p.created_at)=YEAR(CURDATE())",
                        BigDecimal.class, uid(u), uid(u)));
    }

    @GetMapping("/owner/venues")
    @PreAuthorize("hasRole('OWNER')")
    public List<Map<String, Object>> ownerVenues(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList(
                "SELECT v.*,c.name category_name FROM venues v LEFT JOIN categories c ON c.id=v.category_id WHERE v.owner_id=? AND v.status<>'ARCHIVED' ORDER BY v.created_at DESC",
                uid(u));
    }

    @PostMapping("/owner/venues")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> addVenue(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String name = requireText(data, "name", "Tên sân", 180);
        String address = requireText(data, "address", "Địa chỉ", 255);
        BigDecimal price = requirePositivePrice(data);
        Long categoryId = parseOptionalCategoryId(data);
        String city = s(data, "city", "Hà Nội").trim();
        if (city.isEmpty()) city = "Hà Nội";
        long id = insertAndGetId(
                "INSERT INTO venues(owner_id,category_id,name,description,address,district,city,image_url,price_per_hour,status) VALUES(?,?,?,?,?,?,?,?,?,'PENDING')",
                uid(u), categoryId, name, s(data, "description", "").trim(), address, s(data, "district", "").trim(),
                city, s(data, "imageUrl", "").trim(), price);
        audit(u, "VENUE_CREATE", "VENUE", id, null);
        return Map.of("id", id, "status", "PENDING");
    }

    @PutMapping("/owner/venues/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> updateVenue(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String name = requireText(data, "name", "Tên sân", 180);
        String address = requireText(data, "address", "Địa chỉ", 255);
        BigDecimal price = requirePositivePrice(data);
        Long categoryId = parseOptionalCategoryId(data);
        String city = s(data, "city", "Hà Nội").trim();
        if (city.isEmpty()) city = "Hà Nội";
        int updated = db.update(
                "UPDATE venues SET category_id=?,name=?,description=?,address=?,district=?,city=?,image_url=?,price_per_hour=? WHERE id=? AND owner_id=? AND status<>'ARCHIVED'",
                categoryId, name, s(data, "description", "").trim(), address, s(data, "district", "").trim(),
                city, s(data, "imageUrl", "").trim(), price, id, uid(u));
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân hoặc bạn không có quyền sửa");
        audit(u, "VENUE_UPDATE", "VENUE", id, null);
        return Map.of("success", true);
    }

    @DeleteMapping("/owner/venues/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> archiveVenue(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id) {
        int updated = db.update("UPDATE venues SET status='ARCHIVED' WHERE id=? AND owner_id=? AND status<>'ARCHIVED'", id, uid(u));
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân hoặc sân đã được lưu trữ");
        audit(u, "VENUE_ARCHIVE", "VENUE", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/owner/revenue")
    @PreAuthorize("hasRole('OWNER')")
    public List<Map<String, Object>> ownerRevenue(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList(
                "SELECT p.*,b.booking_code,v.name venue_name,a.full_name payer_name FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN venues v ON v.id=b.venue_id JOIN accounts a ON a.id=p.payer_id WHERE (b.owner_id=? OR v.owner_id=?) AND p.status='PAID' ORDER BY p.created_at DESC",
                uid(u), uid(u));
    }

    @GetMapping("/owner/bookings")
    @PreAuthorize("hasRole('OWNER')")
    public List<Map<String, Object>> ownerBookings(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList(
                "SELECT b.*,v.name venue_name,a.full_name player_name,a.phone player_phone,p.method payment_method,p.status payment_status FROM bookings b JOIN venues v ON v.id=b.venue_id JOIN accounts a ON a.id=b.player_id LEFT JOIN payments p ON p.booking_id=b.id WHERE (b.owner_id=? OR v.owner_id=?) ORDER BY b.play_date DESC,b.start_time DESC",
                uid(u), uid(u));
    }

    @PatchMapping("/owner/bookings/{id}/decision")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> decideBooking(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id,
                                             @RequestBody(required = false) Map<String, Object> b) {
        Map<String, Object> data = body(b);
        Object rawAccept = data.get("accept");
        if (rawAccept == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu trường accept (true/false)");
        }
        boolean accept;
        if (rawAccept instanceof Boolean boolVal) {
            accept = boolVal;
        } else {
            String acceptText = String.valueOf(rawAccept).trim().toLowerCase();
            if ("true".equals(acceptText)) {
                accept = true;
            } else if ("false".equals(acceptText)) {
                accept = false;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị accept phải là true hoặc false");
            }
        }
        String note = s(data, "note", "").trim();
        if (note.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ghi chú chủ sân tối đa 500 ký tự");
        }
        var rows = db.queryForList(
                "SELECT b.id, b.player_id, b.venue_id, b.play_date, b.start_time, b.end_time, b.status FROM bookings b JOIN venues v ON v.id=b.venue_id WHERE b.id=? AND (b.owner_id=? OR v.owner_id=?) FOR UPDATE",
                id, uid(u), uid(u));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu đặt sân");
        Map<String, Object> booking = rows.get(0);
        String currentStatus = String.valueOf(booking.get("status")).trim().toUpperCase();
        if (!"PENDING_OWNER".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu không còn ở trạng thái chờ");
        }
        if (accept) {
            Integer overlap = db.queryForObject(
                    "SELECT COUNT(*) FROM bookings WHERE venue_id=? AND play_date=? AND id<>? AND status='CONFIRMED' AND start_time < ? AND end_time > ?",
                    Integer.class, booking.get("venue_id"), booking.get("play_date"), id, booking.get("end_time"), booking.get("start_time"));
            if (overlap != null && overlap > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ này đã có lịch đặt được xác nhận trước đó");
            }
        }
        String next = accept ? "CONFIRMED" : "REJECTED";
        int updated = db.update("UPDATE bookings SET status=?, owner_note=?, owner_id=? WHERE id=? AND status='PENDING_OWNER'",
                next, note, uid(u), id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu không còn ở trạng thái chờ");
        notify(((Number) booking.get("player_id")).longValue(),
                accept ? "Đặt sân được xác nhận" : "Đặt sân bị từ chối",
                accept ? "Chủ sân đã xác nhận lịch đặt của bạn." : "Chủ sân không thể nhận lịch đặt này.");
        audit(u, "BOOKING_" + next, "BOOKING", id, note.isEmpty() ? null : note);
        return Map.of("success", true, "id", id, "status", next);
    }

    @PatchMapping("/owner/bookings/{id}/complete")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> completeBooking(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id) {
        var rows = db.queryForList(
                "SELECT b.id, b.player_id, b.status FROM bookings b JOIN venues v ON v.id=b.venue_id WHERE b.id=? AND (b.owner_id=? OR v.owner_id=?) FOR UPDATE",
                id, uid(u), uid(u));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đặt sân");
        int updated = db.update("UPDATE bookings SET status='COMPLETED', owner_id=? WHERE id=? AND status='CONFIRMED'", uid(u), id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hoàn tất lịch này");
        notify(((Number) rows.get(0).get("player_id")).longValue(),
                "Lịch đặt sân đã hoàn thành",
                "Lịch đặt sân của bạn đã hoàn thành. Bạn có thể gửi đánh giá trải nghiệm ngay bây giờ.");
        audit(u, "BOOKING_COMPLETED", "BOOKING", id, null);
        return Map.of("success", true, "id", id, "status", "COMPLETED");
    }

    @PostMapping("/payments")
    @Transactional
    @PreAuthorize("hasRole('PLAYER')")
    public Map<String, Object> createPayment(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        long bookingId = n(data, "bookingId", 0);
        if (bookingId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã lịch đặt không hợp lệ");
        String method = s(data, "method", "BANK_TRANSFER").trim().toUpperCase();
        if (!VALID_PAYMENT_METHODS.contains(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức không hợp lệ");
        }
        var bookingRows = db.queryForList("SELECT id, status, total_amount FROM bookings WHERE id=? AND player_id=? FOR UPDATE", bookingId, uid(u));
        if (bookingRows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đặt sân");
        Map<String, Object> booking = bookingRows.get(0);
        if (!"CONFIRMED".equals(booking.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ thanh toán khi chủ sân đã xác nhận");
        }
        if (List.of("VNPAY", "MOMO").contains(method)) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Cổng thanh toán sẽ được kích hoạt sau khi cấu hình merchant và callback/IPN");
        }
        var existingPayments = db.queryForList("SELECT id, status FROM payments WHERE booking_id=? FOR UPDATE", bookingId);
        if (!existingPayments.isEmpty()) {
            String existingStatus = String.valueOf(existingPayments.get(0).get("status"));
            if ("PAID".equals(existingStatus) || "REFUND_PENDING".equals(existingStatus) || "REFUNDED".equals(existingStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Lịch đặt này đã được thanh toán hoặc đang xử lý hoàn tiền");
            }
            long existingId = ((Number) existingPayments.get(0).get("id")).longValue();
            db.update("UPDATE payments SET method=?, amount=?, status='PENDING', transaction_ref=? WHERE id=?",
                    method, booking.get("total_amount"), s(data, "transactionRef", "").trim(), existingId);
            audit(u, "PAYMENT_SUBMIT", "PAYMENT", existingId, method);
            return Map.of("id", existingId, "status", "PENDING");
        }
        long id = insertAndGetId(
                "INSERT INTO payments(booking_id,payer_id,method,amount,status,transaction_ref) VALUES(?,?,?,?,'PENDING',?)",
                bookingId, uid(u), method, booking.get("total_amount"), s(data, "transactionRef", "").trim());
        audit(u, "PAYMENT_SUBMIT", "PAYMENT", id, method);
        return Map.of("id", id, "status", "PENDING");
    }

    @GetMapping("/payments/mine")
    @PreAuthorize("hasRole('PLAYER')")
    public List<Map<String, Object>> myPayments(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList(
                "SELECT p.*,b.booking_code,v.name venue_name FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN venues v ON v.id=b.venue_id WHERE p.payer_id=? ORDER BY p.created_at DESC",
                uid(u));
    }

    @PostMapping("/favorites/{venueId}")
    @PreAuthorize("hasRole('PLAYER')")
    public Map<String, Object> toggleFavorite(@AuthenticationPrincipal AuthUser u, @PathVariable("venueId") long venueId) {
        Integer existing = db.queryForObject("SELECT COUNT(*) FROM favorites WHERE player_id=? AND venue_id=?", Integer.class, uid(u), venueId);
        if (existing != null && existing > 0) {
            db.update("DELETE FROM favorites WHERE player_id=? AND venue_id=?", uid(u), venueId);
            return Map.of("favorite", false);
        }
        Integer venueExists = db.queryForObject(
                "SELECT COUNT(*) FROM venues v JOIN accounts a ON a.id=v.owner_id WHERE v.id=? AND v.status='ACTIVE' AND a.status='ACTIVE'",
                Integer.class, venueId);
        if (venueExists == null || venueExists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân");
        }
        db.update("INSERT IGNORE INTO favorites(player_id,venue_id) VALUES(?,?)", uid(u), venueId);
        return Map.of("favorite", true);
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('PLAYER')")
    public List<Map<String, Object>> favorites(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList(
                "SELECT v.*,c.name category_name FROM favorites f JOIN venues v ON v.id=f.venue_id JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id WHERE f.player_id=? AND v.status='ACTIVE' AND a.status='ACTIVE' ORDER BY f.created_at DESC",
                uid(u));
    }

    @PostMapping("/reviews")
    @Transactional
    @PreAuthorize("hasRole('PLAYER')")
    public Map<String, Object> addReview(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        long bookingId = n(data, "bookingId", 0);
        if (bookingId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã lịch đặt không hợp lệ");
        var rows = db.queryForList("SELECT id, venue_id, status FROM bookings WHERE id=? AND player_id=?", bookingId, uid(u));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đặt sân");
        Map<String, Object> booking = rows.get(0);
        if (!"COMPLETED".equals(booking.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ được đánh giá sau khi lịch đặt đã hoàn thành");
        }
        Integer reviewed = db.queryForObject("SELECT COUNT(*) FROM reviews WHERE booking_id=?", Integer.class, bookingId);
        if (reviewed != null && reviewed > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bạn đã đánh giá lịch đặt này rồi");
        }
        int rating;
        try {
            Object rawRating = data.get("rating");
            rating = rawRating == null ? 5 : Integer.parseInt(String.valueOf(rawRating).trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Điểm đánh giá từ 1 đến 5");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Điểm đánh giá từ 1 đến 5");
        }
        Object venueId = booking.get("venue_id");
        db.update("INSERT INTO reviews(booking_id,venue_id,player_id,rating,content) VALUES(?,?,?,?,?)",
                bookingId, venueId, uid(u), rating, s(data, "content", "").trim());
        db.update("UPDATE venues SET rating=COALESCE((SELECT AVG(rating) FROM reviews WHERE venue_id=? AND status='VISIBLE'),0) WHERE id=?",
                venueId, venueId);
        return Map.of("success", true);
    }

    @GetMapping("/reviews/mine")
    @PreAuthorize("hasRole('PLAYER')")
    public List<Map<String, Object>> myReviews(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList(
                "SELECT r.*,v.name venue_name FROM reviews r JOIN venues v ON v.id=r.venue_id WHERE r.player_id=? ORDER BY r.created_at DESC",
                uid(u));
    }

    @GetMapping("/notifications")
    public List<Map<String, Object>> notifications(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList("SELECT * FROM notifications WHERE account_id=? ORDER BY created_at DESC LIMIT 100", uid(u));
    }

    @PatchMapping("/notifications/{id}/read")
    public Map<String, Object> readNotification(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id) {
        db.update("UPDATE notifications SET is_read=1 WHERE id=? AND account_id=?", id, uid(u));
        return Map.of("success", true);
    }

    @GetMapping("/owner/venues/{id}/schedules")
    @PreAuthorize("hasRole('OWNER')")
    public List<Map<String, Object>> ownerSchedules(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id) {
        Integer owns = db.queryForObject("SELECT COUNT(*) FROM venues WHERE id=? AND owner_id=?", Integer.class, id, uid(u));
        if (owns == null || owns == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân");
        return db.queryForList("SELECT * FROM venue_schedules WHERE venue_id=? ORDER BY day_of_week", id);
    }

    @PutMapping("/owner/venues/{id}/schedules")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> saveSchedules(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id,
                                             @RequestBody List<Map<String, Object>> schedules) {
        Integer owns = db.queryForObject("SELECT COUNT(*) FROM venues WHERE id=? AND owner_id=? AND status<>'ARCHIVED'", Integer.class, id, uid(u));
        if (owns == null || owns == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân");
        if (schedules == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách lịch hoạt động không hợp lệ");
        Set<Integer> seenDays = new HashSet<>();
        List<Object[]> validated = new ArrayList<>();
        for (Map<String, Object> x : schedules) {
            if (x == null) continue;
            int day = (int) n(x, "dayOfWeek", 0);
            if (day < 1 || day > 7) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek phải nằm trong khoảng 1 đến 7");
            }
            if (!seenDays.add(day)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trùng lịch cho ngày trong tuần: " + day);
            }
            boolean isClosed = Boolean.parseBoolean(s(x, "isClosed", "false"));
            LocalTime openTime;
            LocalTime closeTime;
            try {
                openTime = LocalTime.parse(s(x, "openTime", "06:00").trim());
                closeTime = LocalTime.parse(s(x, "closeTime", "22:00").trim());
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng giờ mở/đóng cửa không hợp lệ");
            }
            if (!isClosed && !closeTime.isAfter(openTime)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giờ đóng cửa phải sau giờ mở cửa");
            }
            int slotMinutes = (int) n(x, "slotMinutes", 60);
            if (slotMinutes < 15 || slotMinutes > 240) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bước thời lượng phải từ 15 đến 240 phút");
            }
            validated.add(new Object[]{id, day, openTime, closeTime, slotMinutes, isClosed});
        }
        db.update("DELETE FROM venue_schedules WHERE venue_id=?", id);
        for (Object[] row : validated) {
            db.update("INSERT INTO venue_schedules(venue_id,day_of_week,open_time,close_time,slot_minutes,is_closed) VALUES(?,?,?,?,?,?)", row);
        }
        audit(u, "VENUE_SCHEDULE_UPDATE", "VENUE", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/owner/reviews")
    @PreAuthorize("hasRole('OWNER')")
    public List<Map<String, Object>> ownerReviews(@AuthenticationPrincipal AuthUser u) {
        return db.queryForList(
                "SELECT r.*,v.name venue_name,a.full_name player_name FROM reviews r JOIN venues v ON v.id=r.venue_id JOIN accounts a ON a.id=r.player_id WHERE v.owner_id=? ORDER BY r.created_at DESC",
                uid(u));
    }

    @PatchMapping("/owner/reviews/{id}/reply")
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, Object> replyReview(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        int updated = db.update("UPDATE reviews r JOIN venues v ON v.id=r.venue_id SET r.owner_reply=? WHERE r.id=? AND v.owner_id=?",
                s(body(b), "reply", "").trim(), id, uid(u));
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá");
        audit(u, "REVIEW_REPLY", "REVIEW", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal AuthUser u) {
        return db.queryForMap("SELECT id,full_name,email,phone,role,status,created_at FROM accounts WHERE id=?", uid(u));
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String fullName = requireText(data, "fullName", "Họ và tên", 120);
        db.update("UPDATE accounts SET full_name=?,phone=? WHERE id=?", fullName, s(data, "phone", "").trim(), uid(u));
        return Map.of("success", true);
    }

    @PostMapping("/admin/venues")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminCreateVenue(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        long owner = requireValidVenueOwner(data);
        String name = requireText(data, "name", "Tên sân", 180);
        String address = requireText(data, "address", "Địa chỉ", 255);
        BigDecimal price = requirePositivePrice(data);
        Long categoryId = parseOptionalCategoryId(data);
        String city = s(data, "city", "Hà Nội").trim();
        if (city.isEmpty()) city = "Hà Nội";
        long id = insertAndGetId(
                "INSERT INTO venues(owner_id,category_id,name,description,address,district,city,image_url,price_per_hour,status) VALUES(?,?,?,?,?,?,?,?,?,'ACTIVE')",
                owner, categoryId, name, s(data, "description", "").trim(), address, s(data, "district", "").trim(),
                city, s(data, "imageUrl", "").trim(), price);
        audit(u, "ADMIN_VENUE_CREATE", "VENUE", id, null);
        return Map.of("id", id);
    }

    @PutMapping("/admin/venues/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminUpdateVenue(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        long owner = requireValidVenueOwner(data);
        String name = requireText(data, "name", "Tên sân", 180);
        String address = requireText(data, "address", "Địa chỉ", 255);
        BigDecimal price = requirePositivePrice(data);
        Long categoryId = parseOptionalCategoryId(data);
        String city = s(data, "city", "Hà Nội").trim();
        if (city.isEmpty()) city = "Hà Nội";
        int updated = db.update(
                "UPDATE venues SET owner_id=?,category_id=?,name=?,description=?,address=?,district=?,city=?,image_url=?,price_per_hour=? WHERE id=?",
                owner, categoryId, name, s(data, "description", "").trim(), address, s(data, "district", "").trim(),
                city, s(data, "imageUrl", "").trim(), price, id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân");
        db.update("UPDATE bookings SET owner_id=? WHERE venue_id=? AND owner_id<>?", owner, id, owner);
        audit(u, "ADMIN_VENUE_UPDATE", "VENUE", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminDashboard() {
        return Map.of(
                "accounts", db.queryForObject("SELECT COUNT(*) FROM accounts WHERE status<>'DELETED'", Integer.class),
                "venues", db.queryForObject("SELECT COUNT(*) FROM venues WHERE status='ACTIVE'", Integer.class),
                "pendingVenues", db.queryForObject("SELECT COUNT(*) FROM venues WHERE status='PENDING'", Integer.class),
                "pendingBookings", db.queryForObject("SELECT COUNT(*) FROM bookings WHERE status='PENDING_OWNER'", Integer.class),
                "pendingPayments", db.queryForObject("SELECT COUNT(*) FROM payments WHERE status='PENDING'", Integer.class),
                "revenue", db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='PAID'", BigDecimal.class));
    }

    @GetMapping("/admin/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> adminAccounts() {
        return db.queryForList("SELECT id,full_name,email,phone,role,status,created_at FROM accounts ORDER BY created_at DESC");
    }

    @PostMapping("/admin/accounts")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminCreateAccount(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String fullName = requireText(data, "fullName", "Họ và tên", 120);
        String email = requireEmail(data);
        String role = s(data, "role", "PLAYER").trim().toUpperCase();
        if (!VALID_ROLES.contains(role)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ");
        String password = s(data, "password", "");
        if (password.length() < 8 || password.length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải từ 8 đến 72 ký tự");
        }
        Integer exists = db.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, email);
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        long id = insertAndGetId(
                "INSERT INTO accounts(full_name,email,phone,password_hash,role) VALUES(?,?,?,?,?)",
                fullName, email, s(data, "phone", "").trim(), passwordEncoder.encode(password), role);
        audit(u, "ADMIN_ACCOUNT_CREATE", "ACCOUNT", id, role);
        return Map.of("id", id);
    }

    @PutMapping("/admin/accounts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminUpdateAccount(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String fullName = requireText(data, "fullName", "Họ và tên", 120);
        String email = requireEmail(data);
        String role = s(data, "role", "PLAYER").trim().toUpperCase();
        if (!VALID_ROLES.contains(role)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vai trò không hợp lệ");
        if (id == uid(u) && !"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hạ quyền tài khoản quản trị đang đăng nhập");
        }
        Integer duplicate = db.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=? AND id<>?", Integer.class, email, id);
        if (duplicate != null && duplicate > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng bởi tài khoản khác");
        }
        int updated = db.update("UPDATE accounts SET full_name=?,email=?,phone=?,role=? WHERE id=? AND status<>'DELETED'",
                fullName, email, s(data, "phone", "").trim(), role, id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản hoặc tài khoản đã bị vô hiệu hóa");
        audit(u, "ADMIN_ACCOUNT_UPDATE", "ACCOUNT", id, role);
        return Map.of("success", true);
    }

    @PatchMapping("/admin/accounts/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> accountStatus(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        String status = s(body(b), "status", "ACTIVE").trim().toUpperCase();
        if (!VALID_ACCOUNT_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái tài khoản không hợp lệ");
        }
        if (id == uid(u)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể tự thay đổi trạng thái tài khoản đang đăng nhập");
        }
        var rows = db.queryForList("SELECT role FROM accounts WHERE id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        if ("ADMIN".equals(rows.get(0).get("role")) && ("LOCKED".equals(status) || "DELETED".equals(status))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể khóa hoặc vô hiệu hóa tài khoản quản trị");
        }
        db.update("UPDATE accounts SET status=? WHERE id=?", status, id);
        audit(u, "ACCOUNT_" + status, "ACCOUNT", id, null);
        return Map.of("success", true);
    }

    @PatchMapping("/admin/accounts/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> resetAccountPassword(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        String password = s(body(b), "password", "");
        if (password.length() < 8 || password.length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải từ 8 đến 72 ký tự");
        }
        int updated = db.update("UPDATE accounts SET password_hash=? WHERE id=?", passwordEncoder.encode(password), id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        audit(u, "ACCOUNT_PASSWORD_RESET", "ACCOUNT", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/admin/venues")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> adminVenues() {
        return db.queryForList("SELECT v.*,a.full_name owner_name,c.name category_name FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id ORDER BY v.created_at DESC");
    }

    @PatchMapping("/admin/venues/{id}/status")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> venueStatus(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        String status = s(body(b), "status", "ACTIVE").trim().toUpperCase();
        if (!VALID_VENUE_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái sân không hợp lệ");
        }
        var rows = db.queryForList("SELECT owner_id, name FROM venues WHERE id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân");
        db.update("UPDATE venues SET status=? WHERE id=?", status, id);
        long ownerId = ((Number) rows.get(0).get("owner_id")).longValue();
        String venueName = String.valueOf(rows.get(0).get("name"));
        if ("ACTIVE".equals(status)) {
            notify(ownerId, "Sân đã được duyệt", "Sân \"" + venueName + "\" đã được phê duyệt và hiển thị cho khách đặt.");
        } else if ("REJECTED".equals(status)) {
            notify(ownerId, "Sân bị từ chối duyệt", "Sân \"" + venueName + "\" chưa đạt yêu cầu phê duyệt. Vui lòng cập nhật lại thông tin.");
        }
        audit(u, "VENUE_" + status, "VENUE", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> adminBookings() {
        return db.queryForList("SELECT b.*,v.name venue_name,p.full_name player_name,o.full_name owner_name FROM bookings b JOIN venues v ON v.id=b.venue_id JOIN accounts p ON p.id=b.player_id JOIN accounts o ON o.id=b.owner_id ORDER BY b.created_at DESC");
    }

    @PatchMapping("/admin/bookings/{id}/cancel")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminCancelBooking(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id) {
        var rows = db.queryForList("SELECT id, player_id, owner_id, booking_code, status FROM bookings WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đặt sân");
        int updated = db.update("UPDATE bookings SET status='CANCELLED' WHERE id=? AND status IN ('PENDING_OWNER','CONFIRMED')", id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ có thể hủy lịch đang chờ xác nhận hoặc đã xác nhận");
        }
        db.update("UPDATE payments SET status=CASE WHEN status='PAID' THEN 'REFUND_PENDING' ELSE 'CANCELLED' END WHERE booking_id=? AND status IN ('PENDING','PAID')", id);
        Map<String, Object> booking = rows.get(0);
        String code = String.valueOf(booking.get("booking_code"));
        notify(((Number) booking.get("player_id")).longValue(), "Lịch đặt đã bị quản trị viên hủy", "Lịch đặt " + code + " đã bị quản trị viên hủy.");
        notify(((Number) booking.get("owner_id")).longValue(), "Lịch đặt đã bị quản trị viên hủy", "Lịch đặt " + code + " tại sân của bạn đã bị quản trị viên hủy.");
        audit(u, "ADMIN_CANCEL_BOOKING", "BOOKING", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/admin/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> adminPayments() {
        return db.queryForList("SELECT p.*,b.booking_code,v.name venue_name,a.full_name payer_name FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN venues v ON v.id=b.venue_id JOIN accounts a ON a.id=p.payer_id ORDER BY p.created_at DESC");
    }

    @PatchMapping("/admin/payments/{id}/confirm")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> confirmPayment(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id,
                                              @RequestBody(required = false) Map<String, Object> b) {
        var rows = db.queryForList(
                "SELECT p.payer_id, p.booking_id, p.status payment_status, b.status booking_status FROM payments p JOIN bookings b ON b.id=p.booking_id WHERE p.id=? FOR UPDATE",
                id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch thanh toán");
        Map<String, Object> payment = rows.get(0);
        String bookingStatus = String.valueOf(payment.get("booking_status"));
        if (!"CONFIRMED".equals(bookingStatus) && !"COMPLETED".equals(bookingStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể xác nhận thanh toán cho lịch đặt đã hủy hoặc chưa được xác nhận");
        }
        int updated = db.update("UPDATE payments SET status='PAID',confirmed_by=?,admin_note=? WHERE id=? AND status='PENDING'",
                uid(u), s(body(b), "note", "").trim(), id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch không còn chờ xác nhận");
        notify(((Number) payment.get("payer_id")).longValue(), "Thanh toán đã được xác nhận", "Admin đã xác nhận giao dịch thanh toán của bạn.");
        audit(u, "PAYMENT_CONFIRM", "PAYMENT", id, null);
        return Map.of("success", true);
    }

    @PatchMapping("/admin/payments/{id}/refund")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> refundPayment(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id,
                                             @RequestBody(required = false) Map<String, Object> b) {
        var rows = db.queryForList("SELECT payer_id FROM payments WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch thanh toán");
        int updated = db.update("UPDATE payments SET status='REFUNDED',confirmed_by=?,admin_note=? WHERE id=? AND status='REFUND_PENDING'",
                uid(u), s(body(b), "note", "Đã xác nhận hoàn tiền ngoài hệ thống").trim(), id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Khoản tiền chưa ở trạng thái chờ hoàn");
        notify(((Number) rows.get(0).get("payer_id")).longValue(), "Hoàn tiền đã được xác nhận", "Admin đã cập nhật trạng thái hoàn tiền.");
        audit(u, "PAYMENT_REFUND", "PAYMENT", id, null);
        return Map.of("success", true);
    }

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> adminReviews() {
        return db.queryForList("SELECT r.*,v.name venue_name,a.full_name player_name FROM reviews r JOIN venues v ON v.id=r.venue_id JOIN accounts a ON a.id=r.player_id ORDER BY r.created_at DESC");
    }

    @PatchMapping("/admin/reviews/{id}/visibility")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reviewVisibility(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        var rows = db.queryForList("SELECT venue_id FROM reviews WHERE id=?", id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đánh giá");
        Object venueId = rows.get(0).get("venue_id");
        boolean visible = Boolean.parseBoolean(s(body(b), "visible", "false"));
        db.update("UPDATE reviews SET status=? WHERE id=?", visible ? "VISIBLE" : "HIDDEN", id);
        db.update("UPDATE venues SET rating=COALESCE((SELECT AVG(rating) FROM reviews WHERE venue_id=? AND status='VISIBLE'),0) WHERE id=?",
                venueId, venueId);
        audit(u, visible ? "REVIEW_SHOW" : "REVIEW_HIDE", "REVIEW", id, null);
        return Map.of("success", true);
    }

    @PostMapping("/admin/notifications")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> broadcast(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String target = s(data, "target", "ALL").trim().toUpperCase();
        if (!List.of("ALL", "PLAYER", "OWNER").contains(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đối tượng nhận thông báo không hợp lệ");
        }
        String title = requireText(data, "title", "Tiêu đề thông báo", 180);
        String message = requireText(data, "message", "Nội dung thông báo", 1000);
        String sql = "SELECT id FROM accounts WHERE status='ACTIVE'" + ("PLAYER".equals(target) || "OWNER".equals(target) ? " AND role=?" : "");
        List<Long> ids = "PLAYER".equals(target) || "OWNER".equals(target)
                ? db.queryForList(sql, Long.class, target)
                : db.queryForList(sql, Long.class);
        for (Long id : ids) notify(id, title, message);
        audit(u, "NOTIFICATION_BROADCAST", "NOTIFICATION", null, target + " (" + ids.size() + ")");
        return Map.of("sent", ids.size());
    }

    @GetMapping("/admin/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> revenue(@RequestParam(name = "period", defaultValue = "month") String period) {
        String group = switch (period) {
            case "day" -> "DATE(p.created_at)";
            case "year" -> "YEAR(p.created_at)";
            default -> "DATE_FORMAT(p.created_at,'%Y-%m')";
        };
        return db.queryForList("SELECT " + group + " AS period,SUM(p.amount) AS revenue,COUNT(*) AS transactions FROM payments p WHERE p.status='PAID' GROUP BY " + group + " ORDER BY period DESC LIMIT 24");
    }

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> auditLogs() {
        return db.queryForList("SELECT l.*,a.full_name actor_name FROM audit_logs l LEFT JOIN accounts a ON a.id=l.actor_id ORDER BY l.created_at DESC LIMIT 300");
    }

    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> adminCategories() {
        return db.queryForList("SELECT * FROM categories ORDER BY name");
    }

    @PostMapping("/admin/categories")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> addCategory(@AuthenticationPrincipal AuthUser u, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String name = requireText(data, "name", "Tên danh mục", 100);
        Integer exists = db.queryForObject("SELECT COUNT(*) FROM categories WHERE name=?", Integer.class, name);
        if (exists != null && exists > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại");
        }
        long id = insertAndGetId("INSERT INTO categories(name,description) VALUES(?,?)", name, s(data, "description", "").trim());
        audit(u, "CATEGORY_CREATE", "CATEGORY", id, name);
        return Map.of("id", id);
    }

    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateCategory(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id, @RequestBody Map<String, Object> b) {
        Map<String, Object> data = body(b);
        String name = requireText(data, "name", "Tên danh mục", 100);
        Integer duplicate = db.queryForObject("SELECT COUNT(*) FROM categories WHERE name=? AND id<>?", Integer.class, name, id);
        if (duplicate != null && duplicate > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên danh mục đã tồn tại");
        }
        int updated = db.update("UPDATE categories SET name=?,description=? WHERE id=?", name, s(data, "description", "").trim(), id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục");
        audit(u, "CATEGORY_UPDATE", "CATEGORY", id, name);
        return Map.of("success", true);
    }

    @DeleteMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> deleteCategory(@AuthenticationPrincipal AuthUser u, @PathVariable("id") long id) {
        int updated = db.update("UPDATE categories SET active=0 WHERE id=?", id);
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục");
        audit(u, "CATEGORY_DEACTIVATE", "CATEGORY", id, null);
        return Map.of("success", true);
    }
}
