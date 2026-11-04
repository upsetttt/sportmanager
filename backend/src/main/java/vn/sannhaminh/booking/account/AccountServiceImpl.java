package vn.sannhaminh.booking.account;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountDao accountDao;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;

    public AccountServiceImpl(AccountDao accountDao, PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager, JwtEncoder jwtEncoder) {
        this.accountDao = accountDao;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không được vượt quá 72 byte.");
        }
        String email = request.email().trim().toLowerCase();
        if (accountDao.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký.");
        }
        Account account = accountDao.insert(new Account(request.fullName().trim(), email,
                request.phone().trim(), passwordEncoder.encode(request.password()), AccountRole.PLAYER));
        return createResponse(account);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        return createResponse((Account) authentication.getPrincipal());
    }

    @Override
    public AccountResponse profile(String email) {
        return AccountResponse.from(findAccount(email));
    }

    @Override
    @Transactional
    public AccountResponse updateProfile(String email, UpdateProfileRequest request) {
        Account account = findAccount(email);
        account.updateProfile(request.fullName().trim(), request.phone().trim());
        accountDao.updateProfile(account);
        return AccountResponse.from(account);
    }

    private Account findAccount(String email) {
        return accountDao.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn hợp lệ."));
    }

    private AuthResponse createResponse(Account account) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("san-nha-minh")
                .issuedAt(now)
                .expiresAt(now.plus(12, ChronoUnit.HOURS))
                .subject(account.getEmail())
                .claim("role", account.getRole().name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(token, AccountResponse.from(account));
    }
}
