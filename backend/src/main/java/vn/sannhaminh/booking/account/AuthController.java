package vn.sannhaminh.booking.account;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accountService;

    public AuthController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return accountService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return accountService.login(request);
    }

    @GetMapping("/me")
    public AccountResponse profile(@AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {
        return accountService.profile(jwt.getSubject());
    }

    @PutMapping("/me")
    public AccountResponse updateProfile(@AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return accountService.updateProfile(jwt.getSubject(), request);
    }
}
