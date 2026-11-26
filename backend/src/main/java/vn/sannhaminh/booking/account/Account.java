package vn.sannhaminh.booking.account;

import java.time.LocalDateTime;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class Account implements UserDetails {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private AccountRole role;
    private LocalDateTime createdAt;

    public Account(String fullName, String email, String phone, String passwordHash, AccountRole role) {
        this(null, fullName, email, phone, passwordHash, role, LocalDateTime.now());
    }

    public Account(Long id, String fullName, String email, String phone, String passwordHash,
                   AccountRole role, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public AccountRole getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return email; }
}
