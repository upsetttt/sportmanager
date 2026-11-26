package vn.sannhaminh.booking.config;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.sannhaminh.booking.account.Account;
import vn.sannhaminh.booking.account.AccountDao;
import vn.sannhaminh.booking.account.AccountRole;
import vn.sannhaminh.booking.venue.Venue;
import vn.sannhaminh.booking.venue.VenueDao;

@Configuration
public class SeedData {
    @Bean
    CommandLineRunner seedInitialData(AccountDao accountDao, VenueDao venueDao, PasswordEncoder passwordEncoder,
                                      @Value("${app.bootstrap-owner.email}") String ownerEmail,
                                      @Value("${app.bootstrap-owner.password}") String ownerPassword,
                                      @Value("${app.bootstrap-owner.name}") String ownerName,
                                      @Value("${app.bootstrap-owner.phone}") String ownerPhone) {
        return args -> seed(accountDao, venueDao, passwordEncoder, ownerEmail, ownerPassword, ownerName, ownerPhone);
    }

    void seed(AccountDao accountDao, VenueDao venueDao, PasswordEncoder passwordEncoder,
              String ownerEmail, String ownerPassword, String ownerName, String ownerPhone) {
        String email = ownerEmail.trim().toLowerCase();
        int passwordLength = ownerPassword.getBytes(StandardCharsets.UTF_8).length;
        if (email.isBlank() || passwordLength < 8 || passwordLength > 72) {
            throw new IllegalStateException("Configure OWNER_EMAIL and an OWNER_PASSWORD containing 8 to 72 UTF-8 bytes.");
        }

        Account owner = accountDao.findByEmailIgnoreCase(email).orElseGet(() ->
                accountDao.insert(new Account(ownerName.trim(), email, ownerPhone.trim(),
                        passwordEncoder.encode(ownerPassword), AccountRole.OWNER)));
        if (owner.getRole() != AccountRole.OWNER) {
            throw new IllegalStateException("OWNER_EMAIL belongs to a non-owner account; configure a different bootstrap owner email.");
        }

        venueDao.assignOwnerToUnassignedDemoVenues(owner.getId());
        if (venueDao.count() > 0) return;
        venueDao.insert(new Venue("Sân bóng The Pitch", "Bóng đá", "Cầu Giấy, Hà Nội", 350000,
                "https://images.unsplash.com/photo-1522778119026-d647f0596c20?auto=format&fit=crop&w=1000&q=85",
                4.9, 128, "5 người|Cỏ nhân tạo|Có đèn", true, owner));
        venueDao.insert(new Venue("Smash Badminton Club", "Cầu lông", "Đống Đa, Hà Nội", 120000,
                "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?auto=format&fit=crop&w=1000&q=85",
                4.8, 86, "Sàn gỗ|Điều hòa|Cho thuê vợt", false, owner));
        venueDao.insert(new Venue("Riverside Tennis Club", "Tennis", "Tây Hồ, Hà Nội", 280000,
                "https://images.unsplash.com/photo-1595435934249-5df7ed86e1c0?auto=format&fit=crop&w=1000&q=85",
                4.9, 64, "Sân ngoài trời|Phòng tắm|Bãi đỗ xe", false, owner));
        venueDao.insert(new Venue("Green Court Basketball", "Bóng rổ", "Ba Đình, Hà Nội", 200000,
                "https://images.unsplash.com/photo-1504450758481-7338eba7524a?auto=format&fit=crop&w=1000&q=85",
                4.7, 52, "Sân ngoài trời|Bóng miễn phí|Có đèn", false, owner));
    }
}
