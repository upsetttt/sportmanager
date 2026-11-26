package vn.sannhaminh.booking.account;

import java.util.Optional;

public interface AccountDao {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Account insert(Account account);
    void updateProfile(Account account);
}
