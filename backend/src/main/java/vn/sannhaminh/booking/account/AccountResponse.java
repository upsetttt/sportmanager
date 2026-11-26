package vn.sannhaminh.booking.account;

public record AccountResponse(Long id, String fullName, String email, String phone, AccountRole role) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getFullName(), account.getEmail(),
                account.getPhone(), account.getRole());
    }
}
