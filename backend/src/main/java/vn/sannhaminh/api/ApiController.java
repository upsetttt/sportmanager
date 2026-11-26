package vn.sannhaminh.api;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.sannhaminh.security.AuthUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final JdbcTemplate db;
    public ApiController(JdbcTemplate db) { this.db=db; }
    private long uid(AuthUser u){ return u.id(); }
    private void audit(AuthUser u,String action,String type,Long id,String detail){ db.update("INSERT INTO audit_logs(actor_id,action,target_type,target_id,details) VALUES(?,?,?,?,?)",u==null?null:u.id(),action,type,id,detail); }
    private void notify(long account,String title,String message){ db.update("INSERT INTO notifications(account_id,title,message) VALUES(?,?,?)",account,title,message); }
    private Map<String,Object> body(Map<String,Object> b){ return b==null?new HashMap<>():b; }
    private String s(Map<String,Object>b,String k,String d){ Object v=b.get(k); return v==null?d:String.valueOf(v); }
    private long n(Map<String,Object>b,String k,long d){ try{return Long.parseLong(String.valueOf(b.get(k)));}catch(Exception e){return d;} }

    @GetMapping("/health") public Map<String,Object> health(){ return Map.of("status","UP","service","San Nha Minh API"); }
    @GetMapping("/categories") public List<Map<String,Object>> categories(){ return db.queryForList("SELECT * FROM categories WHERE active=1 ORDER BY name"); }

    @GetMapping("/venues") public List<Map<String,Object>> venues(@RequestParam(required=false) String q,@RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) String district,@RequestParam(required=false) String sort){
        String sql="SELECT v.*,a.full_name owner_name,c.name category_name FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id WHERE v.status='ACTIVE' AND a.status='ACTIVE'";
        List<Object> args=new ArrayList<>();
        if(q!=null&&!q.isBlank()){sql+=" AND (v.name LIKE ? OR v.address LIKE ? OR v.district LIKE ?)";String x="%"+q.trim()+"%";args.add(x);args.add(x);args.add(x);}
        if(categoryId!=null){sql+=" AND v.category_id=?";args.add(categoryId);}
        if(district!=null&&!district.isBlank()){sql+=" AND v.district LIKE ?";args.add("%"+district.trim()+"%");}
        sql+=" ORDER BY "+("price_asc".equals(sort)?"v.price_per_hour ASC":"rating".equals(sort)?"v.rating DESC":"v.created_at DESC");
        return db.queryForList(sql,args.toArray());
    }
    /** API phân trang riêng để không phá vỡ hợp đồng mảng JSON mà giao diện TX1 đang sử dụng. */
    @GetMapping("/venues/page") public Map<String,Object> venuesPage(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) String district,
            @RequestParam(defaultValue="created_desc") String sort,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="12") int size) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page phải lớn hơn hoặc bằng 0");
        if (size < 1 || size > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size phải từ 1 đến 50");
        String from = " FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id WHERE v.status='ACTIVE' AND a.status='ACTIVE'";
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) { from += " AND (v.name LIKE ? OR v.address LIKE ? OR v.district LIKE ?)"; String x="%"+q.trim()+"%"; args.add(x); args.add(x); args.add(x); }
        if (categoryId != null) { from += " AND v.category_id=?"; args.add(categoryId); }
        if (district != null && !district.isBlank()) { from += " AND v.district LIKE ?"; args.add("%"+district.trim()+"%"); }
        String order = switch (sort) { case "price_asc" -> "v.price_per_hour ASC"; case "price_desc" -> "v.price_per_hour DESC"; case "rating" -> "v.rating DESC"; default -> "v.created_at DESC"; };
        Integer total = db.queryForObject("SELECT COUNT(*)"+from, Integer.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args); pageArgs.add(size); pageArgs.add(page * size);
        List<Map<String,Object>> content = db.queryForList("SELECT v.*,a.full_name owner_name,c.name category_name"+from+" ORDER BY "+order+" LIMIT ? OFFSET ?", pageArgs.toArray());
        int totalElements = total == null ? 0 : total;
        int totalPages = (int)Math.ceil(totalElements / (double)size);
        return Map.of("content", content, "page", page, "size", size, "totalElements", totalElements, "totalPages", totalPages,
                "first", page == 0, "last", page + 1 >= totalPages);
    }

    @GetMapping("/venues/{id}") public Map<String,Object> venue(@PathVariable long id){
        var rows=db.queryForList("SELECT v.*,a.full_name owner_name,a.phone owner_phone,c.name category_name FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id WHERE v.id=? AND v.status='ACTIVE'",id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sân");
        Map<String,Object> result=rows.get(0); result.put("reviews",db.queryForList("SELECT r.id,r.rating,r.content,r.owner_reply,r.created_at,a.full_name player_name FROM reviews r JOIN accounts a ON a.id=r.player_id WHERE r.venue_id=? AND r.status='VISIBLE' ORDER BY r.created_at DESC",id)); return result;
    }

    @GetMapping("/player/dashboard") @PreAuthorize("hasRole('PLAYER')") public Map<String,Object> playerDashboard(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){
        return Map.of("upcoming",db.queryForList("SELECT b.*,v.name venue_name,v.address FROM bookings b JOIN venues v ON v.id=b.venue_id WHERE b.player_id=? AND b.play_date>=CURDATE() ORDER BY b.play_date,b.start_time LIMIT 6",uid(u)),
                "bookingCount",db.queryForObject("SELECT COUNT(*) FROM bookings WHERE player_id=?",Integer.class,uid(u)),
                "favoriteCount",db.queryForObject("SELECT COUNT(*) FROM favorites WHERE player_id=?",Integer.class,uid(u)));
    }

    @GetMapping("/owner/dashboard") @PreAuthorize("hasRole('OWNER')") public Map<String,Object> ownerDashboard(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){
        return Map.of("venueCount",db.queryForObject("SELECT COUNT(*) FROM venues WHERE owner_id=? AND status<>'ARCHIVED'",Integer.class,uid(u)),
          "pendingCount",db.queryForObject("SELECT COUNT(*) FROM bookings WHERE owner_id=? AND status='PENDING_OWNER'",Integer.class,uid(u)),
          "monthRevenue",db.queryForObject("SELECT COALESCE(SUM(p.amount),0) FROM payments p JOIN bookings b ON b.id=p.booking_id WHERE b.owner_id=? AND p.status='PAID' AND MONTH(p.created_at)=MONTH(CURDATE()) AND YEAR(p.created_at)=YEAR(CURDATE())",BigDecimal.class,uid(u)));
    }
    @GetMapping("/owner/venues") @PreAuthorize("hasRole('OWNER')") public List<Map<String,Object>> ownerVenues(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){ return db.queryForList("SELECT v.*,c.name category_name FROM venues v LEFT JOIN categories c ON c.id=v.category_id WHERE owner_id=? AND status<>'ARCHIVED' ORDER BY created_at DESC",uid(u)); }
    @PostMapping("/owner/venues") @PreAuthorize("hasRole('OWNER')") public Map<String,Object> addVenue(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@RequestBody Map<String,Object> b){
        db.update("INSERT INTO venues(owner_id,category_id,name,description,address,district,city,image_url,price_per_hour,status) VALUES(?,?,?,?,?,?,?,?,?,'PENDING')",uid(u),b.get("categoryId"),s(b,"name","Sân mới"),s(b,"description",""),s(b,"address",""),s(b,"district",""),s(b,"city","Hà Nội"),s(b,"imageUrl",""),new BigDecimal(s(b,"pricePerHour","0")));
        long id=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class); audit(u,"VENUE_CREATE","VENUE",id,null); return Map.of("id",id,"status","PENDING");
    }
    @PutMapping("/owner/venues/{id}") @PreAuthorize("hasRole('OWNER')") public Map<String,Object> updateVenue(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody Map<String,Object>b){
        int n=db.update("UPDATE venues SET category_id=?,name=?,description=?,address=?,district=?,city=?,image_url=?,price_per_hour=? WHERE id=? AND owner_id=? AND status<>'ARCHIVED'",b.get("categoryId"),s(b,"name",""),s(b,"description",""),s(b,"address",""),s(b,"district",""),s(b,"city","Hà Nội"),s(b,"imageUrl",""),new BigDecimal(s(b,"pricePerHour","0")),id,uid(u));
        if(n==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy sân hoặc bạn không có quyền sửa"); audit(u,"VENUE_UPDATE","VENUE",id,null); return Map.of("success",true);
    }
    @DeleteMapping("/owner/venues/{id}") @PreAuthorize("hasRole('OWNER')") public Map<String,Object> archiveVenue(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id){ int n=db.update("UPDATE venues SET status='ARCHIVED' WHERE id=? AND owner_id=?",id,uid(u)); if(n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND); audit(u,"VENUE_ARCHIVE","VENUE",id,null); return Map.of("success",true); }
    @GetMapping("/owner/revenue") @PreAuthorize("hasRole('OWNER')") public List<Map<String,Object>> ownerRevenue(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){return db.queryForList("SELECT p.*,b.booking_code,v.name venue_name,a.full_name payer_name FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN venues v ON v.id=b.venue_id JOIN accounts a ON a.id=p.payer_id WHERE b.owner_id=? AND p.status='PAID' ORDER BY p.created_at DESC",uid(u));}
    @GetMapping("/owner/bookings") @PreAuthorize("hasRole('OWNER')") public List<Map<String,Object>> ownerBookings(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){ return db.queryForList("SELECT b.*,v.name venue_name,a.full_name player_name,a.phone player_phone,p.method payment_method,p.status payment_status FROM bookings b JOIN venues v ON v.id=b.venue_id JOIN accounts a ON a.id=b.player_id LEFT JOIN payments p ON p.booking_id=b.id WHERE b.owner_id=? ORDER BY b.play_date DESC,b.start_time DESC",uid(u)); }
    @PatchMapping("/owner/bookings/{id}/decision") @Transactional @PreAuthorize("hasRole('OWNER')") public Map<String,Object> decideBooking(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody Map<String,Object>b){
        Map<String,Object> booking=db.queryForMap("SELECT * FROM bookings WHERE id=? AND owner_id=?",id,uid(u)); if(!"PENDING_OWNER".equals(booking.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Yêu cầu không còn ở trạng thái chờ");
        boolean accept=Boolean.parseBoolean(s(b,"accept","false")); String next=accept?"CONFIRMED":"REJECTED"; db.update("UPDATE bookings SET status=?,owner_note=? WHERE id=?",next,s(b,"note",""),id);
        notify(((Number)booking.get("player_id")).longValue(),accept?"Đặt sân được xác nhận":"Đặt sân bị từ chối",accept?"Chủ sân đã xác nhận lịch đặt của bạn.":"Chủ sân không thể nhận lịch đặt này."); audit(u,"BOOKING_"+next,"BOOKING",id,null); return Map.of("success",true,"status",next);
    }
    @PatchMapping("/owner/bookings/{id}/complete") @PreAuthorize("hasRole('OWNER')") public Map<String,Object> completeBooking(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id){ int n=db.update("UPDATE bookings SET status='COMPLETED' WHERE id=? AND owner_id=? AND status='CONFIRMED'",id,uid(u)); if(n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể hoàn tất lịch này"); return Map.of("success",true); }

    @PostMapping("/payments") @PreAuthorize("hasRole('PLAYER')") public Map<String,Object> createPayment(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@RequestBody Map<String,Object>b){
        long bookingId=n(b,"bookingId",0); String method=s(b,"method","BANK_TRANSFER"); if(!List.of("BANK_TRANSFER","CASH","VNPAY","MOMO").contains(method))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Phương thức không hợp lệ");
        Map<String,Object> booking=db.queryForMap("SELECT * FROM bookings WHERE id=? AND player_id=?",bookingId,uid(u)); if(!"CONFIRMED".equals(booking.get("status")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chỉ thanh toán khi chủ sân đã xác nhận");
        if(List.of("VNPAY","MOMO").contains(method))throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,"Cổng thanh toán sẽ được kích hoạt sau khi cấu hình merchant và callback/IPN");
        db.update("INSERT INTO payments(booking_id,payer_id,method,amount,status,transaction_ref) VALUES(?,?,?,?, 'PENDING',?) ON DUPLICATE KEY UPDATE method=VALUES(method),status='PENDING',transaction_ref=VALUES(transaction_ref)",bookingId,uid(u),method,booking.get("total_amount"),s(b,"transactionRef","")); long id=db.queryForObject("SELECT id FROM payments WHERE booking_id=?",Long.class,bookingId); audit(u,"PAYMENT_SUBMIT","PAYMENT",id,method); return Map.of("id",id,"status","PENDING");
    }
    @GetMapping("/payments/mine") @PreAuthorize("hasRole('PLAYER')") public List<Map<String,Object>> myPayments(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){return db.queryForList("SELECT p.*,b.booking_code,v.name venue_name FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN venues v ON v.id=b.venue_id WHERE p.payer_id=? ORDER BY p.created_at DESC",uid(u));}

    @PostMapping("/favorites/{venueId}") @PreAuthorize("hasRole('PLAYER')") public Map<String,Object> toggleFavorite(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long venueId){ Integer c=db.queryForObject("SELECT COUNT(*) FROM favorites WHERE player_id=? AND venue_id=?",Integer.class,uid(u),venueId); if(c!=null&&c>0)db.update("DELETE FROM favorites WHERE player_id=? AND venue_id=?",uid(u),venueId);else db.update("INSERT IGNORE INTO favorites(player_id,venue_id) VALUES(?,?)",uid(u),venueId); return Map.of("favorite",c==null||c==0); }
    @GetMapping("/favorites") @PreAuthorize("hasRole('PLAYER')") public List<Map<String,Object>> favorites(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){return db.queryForList("SELECT v.* FROM favorites f JOIN venues v ON v.id=f.venue_id WHERE f.player_id=? AND v.status='ACTIVE'",uid(u));}
    @PostMapping("/reviews") @PreAuthorize("hasRole('PLAYER')") public Map<String,Object> addReview(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@RequestBody Map<String,Object>b){ long booking=n(b,"bookingId",0); Map<String,Object>x=db.queryForMap("SELECT * FROM bookings WHERE id=? AND player_id=? AND status='COMPLETED'",booking,uid(u)); int rating=(int)n(b,"rating",5); if(rating<1||rating>5)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Điểm đánh giá từ 1 đến 5"); db.update("INSERT INTO reviews(booking_id,venue_id,player_id,rating,content) VALUES(?,?,?,?,?)",booking,x.get("venue_id"),uid(u),rating,s(b,"content","")); db.update("UPDATE venues SET rating=(SELECT AVG(rating) FROM reviews WHERE venue_id=? AND status='VISIBLE') WHERE id=?",x.get("venue_id"),x.get("venue_id")); return Map.of("success",true); }
    @GetMapping("/reviews/mine") @PreAuthorize("hasRole('PLAYER')") public List<Map<String,Object>> myReviews(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){return db.queryForList("SELECT r.*,v.name venue_name FROM reviews r JOIN venues v ON v.id=r.venue_id WHERE r.player_id=? ORDER BY r.created_at DESC",uid(u));}
    @GetMapping("/notifications") public List<Map<String,Object>> notifications(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){return db.queryForList("SELECT * FROM notifications WHERE account_id=? ORDER BY created_at DESC LIMIT 100",uid(u));}
    @PatchMapping("/notifications/{id}/read") public Map<String,Object> readNotification(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id){db.update("UPDATE notifications SET is_read=1 WHERE id=? AND account_id=?",id,uid(u));return Map.of("success",true);}

    @GetMapping("/owner/venues/{id}/schedules") @PreAuthorize("hasRole('OWNER')") public List<Map<String,Object>> ownerSchedules(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id){
        Integer owns=db.queryForObject("SELECT COUNT(*) FROM venues WHERE id=? AND owner_id=?",Integer.class,id,uid(u)); if(owns==null||owns==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return db.queryForList("SELECT * FROM venue_schedules WHERE venue_id=? ORDER BY day_of_week",id);
    }
    @PutMapping("/owner/venues/{id}/schedules") @Transactional @PreAuthorize("hasRole('OWNER')") public Map<String,Object> saveSchedules(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody List<Map<String,Object>> schedules){
        Integer owns=db.queryForObject("SELECT COUNT(*) FROM venues WHERE id=? AND owner_id=?",Integer.class,id,uid(u)); if(owns==null||owns==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        db.update("DELETE FROM venue_schedules WHERE venue_id=?",id);
        for(Map<String,Object> x:schedules){int day=(int)n(x,"dayOfWeek",1);if(day<1||day>7)continue;db.update("INSERT INTO venue_schedules(venue_id,day_of_week,open_time,close_time,slot_minutes,is_closed) VALUES(?,?,?,?,?,?)",id,day,s(x,"openTime","06:00"),s(x,"closeTime","22:00"),(int)n(x,"slotMinutes",60),Boolean.parseBoolean(s(x,"isClosed","false")));}
        audit(u,"VENUE_SCHEDULE_UPDATE","VENUE",id,null);return Map.of("success",true);
    }
    @GetMapping("/owner/reviews") @PreAuthorize("hasRole('OWNER')") public List<Map<String,Object>> ownerReviews(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){return db.queryForList("SELECT r.*,v.name venue_name,a.full_name player_name FROM reviews r JOIN venues v ON v.id=r.venue_id JOIN accounts a ON a.id=r.player_id WHERE v.owner_id=? ORDER BY r.created_at DESC",uid(u));}
    @PatchMapping("/owner/reviews/{id}/reply") @PreAuthorize("hasRole('OWNER')") public Map<String,Object> replyReview(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody Map<String,Object>b){int n=db.update("UPDATE reviews r JOIN venues v ON v.id=r.venue_id SET r.owner_reply=? WHERE r.id=? AND v.owner_id=?",s(b,"reply",""),id,uid(u));if(n==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND);return Map.of("success",true);}
    @GetMapping("/profile") public Map<String,Object> profile(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u){return db.queryForMap("SELECT id,full_name,email,phone,role,status,created_at FROM accounts WHERE id=?",uid(u));}
    @PutMapping("/profile") public Map<String,Object> updateProfile(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@RequestBody Map<String,Object>b){db.update("UPDATE accounts SET full_name=?,phone=? WHERE id=?",s(b,"fullName",u.fullName()),s(b,"phone",""),uid(u));return Map.of("success",true);}

    @PostMapping("/admin/venues") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> adminCreateVenue(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@RequestBody Map<String,Object>b){
        long owner=n(b,"ownerId",0);db.update("INSERT INTO venues(owner_id,category_id,name,description,address,district,city,image_url,price_per_hour,status) VALUES(?,?,?,?,?,?,?,?,?,'ACTIVE')",owner,b.get("categoryId"),s(b,"name","Sân mới"),s(b,"description",""),s(b,"address",""),s(b,"district",""),s(b,"city","Hà Nội"),s(b,"imageUrl",""),new BigDecimal(s(b,"pricePerHour","0")));long id=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class);audit(u,"ADMIN_VENUE_CREATE","VENUE",id,null);return Map.of("id",id);
    }
    @PutMapping("/admin/venues/{id}") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> adminUpdateVenue(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody Map<String,Object>b){
        db.update("UPDATE venues SET owner_id=?,category_id=?,name=?,description=?,address=?,district=?,city=?,image_url=?,price_per_hour=? WHERE id=?",n(b,"ownerId",0),b.get("categoryId"),s(b,"name",""),s(b,"description",""),s(b,"address",""),s(b,"district",""),s(b,"city","Hà Nội"),s(b,"imageUrl",""),new BigDecimal(s(b,"pricePerHour","0")),id);audit(u,"ADMIN_VENUE_UPDATE","VENUE",id,null);return Map.of("success",true);
    }

    @GetMapping("/admin/dashboard") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> adminDashboard(){return Map.of(
      "accounts",db.queryForObject("SELECT COUNT(*) FROM accounts WHERE status<>'DELETED'",Integer.class),"venues",db.queryForObject("SELECT COUNT(*) FROM venues WHERE status='ACTIVE'",Integer.class),
      "pendingVenues",db.queryForObject("SELECT COUNT(*) FROM venues WHERE status='PENDING'",Integer.class),"pendingBookings",db.queryForObject("SELECT COUNT(*) FROM bookings WHERE status='PENDING_OWNER'",Integer.class),
      "pendingPayments",db.queryForObject("SELECT COUNT(*) FROM payments WHERE status='PENDING'",Integer.class),"revenue",db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='PAID'",BigDecimal.class));}
    @GetMapping("/admin/accounts") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> adminAccounts(){return db.queryForList("SELECT id,full_name,email,phone,role,status,created_at FROM accounts ORDER BY created_at DESC");}
    @PostMapping("/admin/accounts") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> adminCreateAccount(@RequestBody Map<String,Object>b,org.springframework.security.crypto.password.PasswordEncoder enc){String role=s(b,"role","PLAYER"); if(!List.of("PLAYER","OWNER","ADMIN").contains(role))throw new ResponseStatusException(HttpStatus.BAD_REQUEST); db.update("INSERT INTO accounts(full_name,email,phone,password_hash,role) VALUES(?,?,?,?,?)",s(b,"fullName",""),s(b,"email","").toLowerCase(),s(b,"phone",""),enc.encode(s(b,"password","ChangeMe123!")),role); long id=db.queryForObject("SELECT LAST_INSERT_ID()",Long.class); return Map.of("id",id);}
    @PutMapping("/admin/accounts/{id}") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> adminUpdateAccount(@PathVariable long id,@RequestBody Map<String,Object>b){db.update("UPDATE accounts SET full_name=?,email=?,phone=?,role=? WHERE id=? AND status<>'DELETED'",s(b,"fullName",""),s(b,"email","").toLowerCase(),s(b,"phone",""),s(b,"role","PLAYER"),id);return Map.of("success",true);}
    @PatchMapping("/admin/accounts/{id}/status") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> accountStatus(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody Map<String,Object>b){String status=s(b,"status","ACTIVE");if(!List.of("ACTIVE","LOCKED","DELETED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);if(id==uid(u))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể tự khóa tài khoản đang đăng nhập");if("ADMIN".equals(db.queryForObject("SELECT role FROM accounts WHERE id=?",String.class,id))&&"LOCKED".equals(status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể khóa tài khoản quản trị");db.update("UPDATE accounts SET status=? WHERE id=?",status,id);audit(u,"ACCOUNT_"+status,"ACCOUNT",id,null);return Map.of("success",true);}
    @PatchMapping("/admin/accounts/{id}/password") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> resetAccountPassword(@PathVariable long id,@RequestBody Map<String,Object>b,org.springframework.security.crypto.password.PasswordEncoder enc){String password=s(b,"password","");if(password.length()<8)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mật khẩu tối thiểu 8 ký tự");db.update("UPDATE accounts SET password_hash=? WHERE id=?",enc.encode(password),id);return Map.of("success",true);}
    @GetMapping("/admin/venues") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> adminVenues(){return db.queryForList("SELECT v.*,a.full_name owner_name,c.name category_name FROM venues v JOIN accounts a ON a.id=v.owner_id LEFT JOIN categories c ON c.id=v.category_id ORDER BY v.created_at DESC");}
    @PatchMapping("/admin/venues/{id}/status") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> venueStatus(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody Map<String,Object>b){String status=s(b,"status","ACTIVE");if(!List.of("ACTIVE","REJECTED","ARCHIVED").contains(status))throw new ResponseStatusException(HttpStatus.BAD_REQUEST);db.update("UPDATE venues SET status=? WHERE id=?",status,id);audit(u,"VENUE_"+status,"VENUE",id,null);return Map.of("success",true);}
    @GetMapping("/admin/bookings") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> adminBookings(){return db.queryForList("SELECT b.*,v.name venue_name,p.full_name player_name,o.full_name owner_name FROM bookings b JOIN venues v ON v.id=b.venue_id JOIN accounts p ON p.id=b.player_id JOIN accounts o ON o.id=b.owner_id ORDER BY b.created_at DESC");}
    @PatchMapping("/admin/bookings/{id}/cancel") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> adminCancelBooking(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id){db.update("UPDATE bookings SET status='CANCELLED' WHERE id=? AND status IN ('PENDING_OWNER','CONFIRMED')",id);db.update("UPDATE payments SET status=CASE WHEN status='PAID' THEN 'REFUND_PENDING' ELSE 'CANCELLED' END WHERE booking_id=?",id);audit(u,"ADMIN_CANCEL_BOOKING","BOOKING",id,null);return Map.of("success",true);}
    @GetMapping("/admin/payments") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> adminPayments(){return db.queryForList("SELECT p.*,b.booking_code,v.name venue_name,a.full_name payer_name FROM payments p JOIN bookings b ON b.id=p.booking_id JOIN venues v ON v.id=b.venue_id JOIN accounts a ON a.id=p.payer_id ORDER BY p.created_at DESC");}
    @PatchMapping("/admin/payments/{id}/confirm") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> confirmPayment(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody(required=false) Map<String,Object>b){Map<String,Object> payment=db.queryForMap("SELECT payer_id,booking_id FROM payments WHERE id=?",id);int n=db.update("UPDATE payments SET status='PAID',confirmed_by=?,admin_note=? WHERE id=? AND status='PENDING'",uid(u),s(body(b),"note",""),id);if(n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Giao dịch không còn chờ xác nhận");notify(((Number)payment.get("payer_id")).longValue(),"Thanh toán đã được xác nhận","Admin đã xác nhận giao dịch thanh toán của bạn.");audit(u,"PAYMENT_CONFIRM","PAYMENT",id,null);return Map.of("success",true);}
    @PatchMapping("/admin/payments/{id}/refund") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> refundPayment(@org.springframework.security.core.annotation.AuthenticationPrincipal AuthUser u,@PathVariable long id,@RequestBody(required=false) Map<String,Object>b){Map<String,Object> payment=db.queryForMap("SELECT payer_id FROM payments WHERE id=?",id);int n=db.update("UPDATE payments SET status='REFUNDED',confirmed_by=?,admin_note=? WHERE id=? AND status='REFUND_PENDING'",uid(u),s(body(b),"note","Đã xác nhận hoàn tiền ngoài hệ thống"),id);if(n==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Khoản tiền chưa ở trạng thái chờ hoàn");notify(((Number)payment.get("payer_id")).longValue(),"Hoàn tiền đã được xác nhận","Admin đã cập nhật trạng thái hoàn tiền.");audit(u,"PAYMENT_REFUND","PAYMENT",id,null);return Map.of("success",true);}
    @GetMapping("/admin/reviews") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> adminReviews(){return db.queryForList("SELECT r.*,v.name venue_name,a.full_name player_name FROM reviews r JOIN venues v ON v.id=r.venue_id JOIN accounts a ON a.id=r.player_id ORDER BY r.created_at DESC");}
    @PatchMapping("/admin/reviews/{id}/visibility") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> reviewVisibility(@PathVariable long id,@RequestBody Map<String,Object>b){Map<String,Object> review=db.queryForMap("SELECT venue_id FROM reviews WHERE id=?",id);db.update("UPDATE reviews SET status=? WHERE id=?",Boolean.parseBoolean(s(b,"visible","false"))?"VISIBLE":"HIDDEN",id);db.update("UPDATE venues SET rating=COALESCE((SELECT AVG(rating) FROM reviews WHERE venue_id=? AND status='VISIBLE'),0) WHERE id=?",review.get("venue_id"),review.get("venue_id"));return Map.of("success",true);}
    @PostMapping("/admin/notifications") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> broadcast(@RequestBody Map<String,Object>b){String target=s(b,"target","ALL");String sql="SELECT id FROM accounts WHERE status='ACTIVE'"+("PLAYER".equals(target)||"OWNER".equals(target)?" AND role=?":"");List<Long> ids="PLAYER".equals(target)||"OWNER".equals(target)?db.queryForList(sql,Long.class,target):db.queryForList(sql,Long.class);for(Long id:ids)notify(id,s(b,"title","Thông báo hệ thống"),s(b,"message",""));return Map.of("sent",ids.size());}
    @GetMapping("/admin/revenue") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> revenue(@RequestParam(defaultValue="month") String period){String group=switch(period){case "day"->"DATE(p.created_at)";case "year"->"YEAR(p.created_at)";default->"DATE_FORMAT(p.created_at,'%Y-%m')";};return db.queryForList("SELECT "+group+" AS period,SUM(p.amount) AS revenue,COUNT(*) AS transactions FROM payments p WHERE p.status='PAID' GROUP BY period ORDER BY period DESC LIMIT 24");}
    @GetMapping("/admin/audit-logs") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> auditLogs(){return db.queryForList("SELECT l.*,a.full_name actor_name FROM audit_logs l LEFT JOIN accounts a ON a.id=l.actor_id ORDER BY l.created_at DESC LIMIT 300");}
    @GetMapping("/admin/categories") @PreAuthorize("hasRole('ADMIN')") public List<Map<String,Object>> adminCategories(){return db.queryForList("SELECT * FROM categories ORDER BY name");}
    @PostMapping("/admin/categories") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> addCategory(@RequestBody Map<String,Object>b){db.update("INSERT INTO categories(name,description) VALUES(?,?)",s(b,"name",""),s(b,"description",""));return Map.of("id",db.queryForObject("SELECT LAST_INSERT_ID()",Long.class));}
    @PutMapping("/admin/categories/{id}") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> updateCategory(@PathVariable long id,@RequestBody Map<String,Object>b){db.update("UPDATE categories SET name=?,description=? WHERE id=?",s(b,"name",""),s(b,"description",""),id);return Map.of("success",true);}
    @DeleteMapping("/admin/categories/{id}") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> deleteCategory(@PathVariable long id){db.update("UPDATE categories SET active=0 WHERE id=?",id);return Map.of("success",true);}
}
