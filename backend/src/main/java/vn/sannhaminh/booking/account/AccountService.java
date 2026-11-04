package vn.sannhaminh.booking.account;

public interface AccountService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AccountResponse profile(String email);
    AccountResponse updateProfile(String email, UpdateProfileRequest request);
}
