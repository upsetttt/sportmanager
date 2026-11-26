package vn.sannhaminh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
<<<<<<< HEAD
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
=======
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
<<<<<<< HEAD

=======
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
<<<<<<< HEAD
    private final ObjectProvider<JdbcTemplate> dbProvider;

    public JwtAuthFilter(JwtService jwtService, ObjectProvider<JdbcTemplate> dbProvider) {
        this.jwtService = jwtService;
        this.dbProvider = dbProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthUser user = jwtService.parse(header.substring(7));
                JdbcTemplate db = dbProvider.getIfAvailable();
                if (db != null) {
                    var rows = db.query(
                            "SELECT full_name, email, role, status FROM accounts WHERE id=?",
                            (rs, n) -> new Object[]{rs.getString("full_name"), rs.getString("email"), rs.getString("role"), rs.getString("status")},
                            user.id());
                    if (rows.isEmpty() || !"ACTIVE".equals(rows.get(0)[3])) {
                        SecurityContextHolder.clearContext();
                        chain.doFilter(request, response);
                        return;
                    }
                    Object[] row = rows.get(0);
                    user = new AuthUser(user.id(), (String) row[1], (String) row[2], (String) row[0]);
                }
                var authentication = new UsernamePasswordAuthenticationToken(
                    user, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role().toUpperCase()))
                );
                authentication.setDetails(user);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
            }
=======
    public JwtAuthFilter(JwtService jwtService) { this.jwtService = jwtService; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                AuthUser user = jwtService.parse(header.substring(7));
                var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) { SecurityContextHolder.clearContext(); }
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
        }
        chain.doFilter(request, response);
    }
}
