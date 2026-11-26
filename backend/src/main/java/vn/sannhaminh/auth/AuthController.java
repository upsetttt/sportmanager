package vn.sannhaminh.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.sannhaminh.security.AuthUser;
import vn.sannhaminh.security.JwtService;
import java.util.Map;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate db; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthController(JdbcTemplate db, PasswordEncoder encoder, JwtService jwt) { this.db=db; this.encoder=encoder; this.jwt=jwt; }
    public record RegisterRequest(@NotBlank @Size(max=120) String fullName, @Email @NotBlank String email,
                                  String phone, @NotBlank @Size(min=8,max=72) String password, String role) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    @PostMapping("/register") public Map<String,Object> register(@Valid @RequestBody RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        String fullName = r.fullName().trim();
        String phone = r.phone() == null ? null : r.phone().trim();
        Integer count=db.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?",Integer.class,email);
        if(count!=null && count>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email đã được sử dụng");
        String role="OWNER".equalsIgnoreCase(r.role())?"OWNER":"PLAYER";
        db.update("INSERT INTO accounts(full_name,email,phone,password_hash,role) VALUES(?,?,?,?,?)",fullName,email,phone,encoder.encode(r.password()),role);
        return login(new LoginRequest(email,r.password()));
    }
    @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody LoginRequest r) {
        String email = r.email().trim().toLowerCase();
        var rows=db.query("SELECT id,full_name,email,password_hash,role,status FROM accounts WHERE email=?",(rs,n)->new Object[]{rs.getLong("id"),rs.getString("full_name"),rs.getString("email"),rs.getString("password_hash"),rs.getString("role"),rs.getString("status")},email);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email hoặc mật khẩu không chính xác");
        Object[] a=rows.get(0); if(!encoder.matches(r.password(),(String)a[3])) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Email hoặc mật khẩu không chính xác");
        if(!"ACTIVE".equals(a[5])) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tài khoản đang bị khóa hoặc vô hiệu hóa");
        AuthUser u=new AuthUser((Long)a[0],(String)a[2],(String)a[4],(String)a[1]);
        return Map.of("token",jwt.create(u),"user",Map.of("id",u.id(),"fullName",u.fullName(),"email",u.email(),"role",u.role()));
    }
}
