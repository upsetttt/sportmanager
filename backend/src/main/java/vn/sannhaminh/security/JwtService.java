package vn.sannhaminh.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;
    public JwtService(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-hours:12}") long hours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = hours * 60 * 60 * 1000;
    }
    public String create(AuthUser user) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(user.id())).claim("email", user.email())
                .claim("role", user.role()).claim("name", user.fullName()).issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs)).signWith(key).compact();
    }
    public AuthUser parse(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthUser(Long.parseLong(claims.getSubject()), claims.get("email", String.class),
                claims.get("role", String.class), claims.get("name", String.class));
    }
}
