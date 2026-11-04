package vn.sannhaminh.booking.venue;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import vn.sannhaminh.booking.account.Account;

@Repository
public class JdbcVenueDao implements VenueDao {
    private static final String VENUE_COLUMNS = """
            SELECT id, name, sport, address, price_per_hour, image_url, rating,
                   review_count, amenities, featured, owner_id, active
            FROM venues
            """;

    private static final RowMapper<Venue> VENUE_ROW_MAPPER = (resultSet, rowNumber) -> {
        long ownerId = resultSet.getLong("owner_id");
        Account owner = resultSet.wasNull() ? null : new Account(ownerId, null, null, null, null, null, null);
        return new Venue(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("sport"),
                resultSet.getString("address"),
                resultSet.getLong("price_per_hour"),
                resultSet.getString("image_url"),
                resultSet.getDouble("rating"),
                resultSet.getInt("review_count"),
                resultSet.getString("amenities"),
                resultSet.getBoolean("featured"),
                owner,
                resultSet.getBoolean("active")
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcVenueDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Venue> findAllActive() {
        return jdbcTemplate.query(VENUE_COLUMNS + " WHERE active = TRUE ORDER BY id", VENUE_ROW_MAPPER);
    }

    @Override
    public List<Venue> findAllActiveByOwnerId(Long ownerId) {
        return jdbcTemplate.query(VENUE_COLUMNS + " WHERE owner_id = ? AND active = TRUE ORDER BY id",
                VENUE_ROW_MAPPER, ownerId);
    }

    @Override
    public Optional<Venue> findById(Long id) {
        return jdbcTemplate.query(VENUE_COLUMNS + " WHERE id = ?", VENUE_ROW_MAPPER, id)
                .stream().findFirst();
    }

    @Override
    public Optional<Venue> findByIdForUpdate(Long id) {
        return jdbcTemplate.query(VENUE_COLUMNS + " WHERE id = ? FOR UPDATE", VENUE_ROW_MAPPER, id)
                .stream().findFirst();
    }

    @Override
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM venues", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public Venue insert(Venue venue) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO venues (name, sport, address, price_per_hour, image_url, rating, "
                            + "review_count, amenities, featured, owner_id, active) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, venue.getName());
            statement.setString(2, venue.getSport());
            statement.setString(3, venue.getAddress());
            statement.setLong(4, venue.getPricePerHour());
            statement.setString(5, venue.getImageUrl());
            statement.setDouble(6, venue.getRating());
            statement.setInt(7, venue.getReviewCount());
            statement.setString(8, venue.getAmenities());
            statement.setBoolean(9, venue.isFeatured());
            if (venue.getOwner() == null) {
                statement.setNull(10, java.sql.Types.BIGINT);
            } else {
                statement.setLong(10, venue.getOwner().getId());
            }
            statement.setBoolean(11, venue.isActive());
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("Database did not return the new venue ID.");
        }
        venue.setId(id.longValue());
        return venue;
    }

    @Override
    public void update(Venue venue) {
        jdbcTemplate.update("""
                UPDATE venues
                SET name = ?, sport = ?, address = ?, price_per_hour = ?, image_url = ?, amenities = ?
                WHERE id = ?
                """, venue.getName(), venue.getSport(), venue.getAddress(), venue.getPricePerHour(),
                venue.getImageUrl(), venue.getAmenities(), venue.getId());
    }

    @Override
    public void archive(Long id) {
        jdbcTemplate.update("UPDATE venues SET active = FALSE WHERE id = ?", id);
    }

    @Override
    public void assignOwnerToUnassignedDemoVenues(Long ownerId) {
        jdbcTemplate.update("""
                UPDATE venues SET owner_id = ?
                WHERE owner_id IS NULL
                  AND name IN (?, ?, ?, ?)
                """, ownerId, "Sân bóng The Pitch", "Smash Badminton Club",
                "Riverside Tennis Club", "Green Court Basketball");
    }
}
