package vn.sannhaminh.booking.account;

import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountDao implements AccountDao {
    private static final RowMapper<Account> ACCOUNT_ROW_MAPPER = (resultSet, rowNumber) ->
            new Account(
                    resultSet.getLong("id"),
                    resultSet.getString("full_name"),
                    resultSet.getString("email"),
                    resultSet.getString("phone"),
                    resultSet.getString("password_hash"),
                    AccountRole.valueOf(resultSet.getString("role")),
                    resultSet.getTimestamp("created_at").toLocalDateTime()
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Account> findByEmailIgnoreCase(String email) {
        List<Account> accounts = jdbcTemplate.query(
                "SELECT id, full_name, email, phone, password_hash, role, created_at "
                        + "FROM accounts WHERE LOWER(email) = LOWER(?)",
                ACCOUNT_ROW_MAPPER, email);
        return accounts.stream().findFirst();
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM accounts WHERE LOWER(email) = LOWER(?)", Integer.class, email);
        return count != null && count > 0;
    }

    @Override
    public Account insert(Account account) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO accounts (full_name, email, phone, password_hash, role, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, account.getFullName());
            statement.setString(2, account.getEmail());
            statement.setString(3, account.getPhone());
            statement.setString(4, account.getPassword());
            statement.setString(5, account.getRole().name());
            statement.setTimestamp(6, Timestamp.valueOf(account.getCreatedAt()));
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("Database did not return the new account ID.");
        }
        account.setId(id.longValue());
        return account;
    }

    @Override
    public void updateProfile(Account account) {
        jdbcTemplate.update("UPDATE accounts SET full_name = ?, phone = ? WHERE id = ?",
                account.getFullName(), account.getPhone(), account.getId());
    }
}
