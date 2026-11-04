package vn.sannhaminh.booking.booking;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import vn.sannhaminh.booking.venue.Venue;

@Repository
public class JdbcBookingDao implements BookingDao {
    private static final String BOOKING_COLUMNS = """
            SELECT b.id AS booking_id, b.customer_name, b.phone, b.email, b.booking_date,
                   b.start_time, b.end_time, b.total_price, b.status, b.created_at,
                   v.id AS venue_id, v.name AS venue_name, v.sport AS venue_sport,
                   v.address AS venue_address, v.price_per_hour, v.image_url, v.rating,
                   v.review_count, v.amenities, v.featured, v.owner_id, v.active
            FROM bookings b
            JOIN venues v ON v.id = b.venue_id
            """;

    private static final RowMapper<Booking> BOOKING_ROW_MAPPER = (resultSet, rowNumber) -> {
        long ownerId = resultSet.getLong("owner_id");
        var owner = resultSet.wasNull() ? null
                : new vn.sannhaminh.booking.account.Account(ownerId, null, null, null, null, null, null);
        Venue venue = new Venue(
                resultSet.getLong("venue_id"),
                resultSet.getString("venue_name"),
                resultSet.getString("venue_sport"),
                resultSet.getString("venue_address"),
                resultSet.getLong("price_per_hour"),
                resultSet.getString("image_url"),
                resultSet.getDouble("rating"),
                resultSet.getInt("review_count"),
                resultSet.getString("amenities"),
                resultSet.getBoolean("featured"),
                owner,
                resultSet.getBoolean("active")
        );
        return new Booking(
                resultSet.getLong("booking_id"),
                venue,
                resultSet.getString("customer_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                resultSet.getDate("booking_date").toLocalDate(),
                resultSet.getTime("start_time").toLocalTime(),
                resultSet.getTime("end_time").toLocalTime(),
                resultSet.getLong("total_price"),
                BookingStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toLocalDateTime()
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcBookingDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsOverlappingBooking(Long venueId, LocalDate bookingDate,
                                            LocalTime startTime, LocalTime endTime) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM bookings
                WHERE venue_id = ? AND booking_date = ? AND status = ?
                  AND start_time < ? AND end_time > ?
                """, Integer.class, venueId, Date.valueOf(bookingDate), BookingStatus.CONFIRMED.name(),
                Time.valueOf(endTime), Time.valueOf(startTime));
        return count != null && count > 0;
    }

    @Override
    public List<Booking> findByEmailIgnoreCaseOrderByBookingDateAscStartTimeAsc(String email) {
        return jdbcTemplate.query(BOOKING_COLUMNS + """
                WHERE LOWER(b.email) = LOWER(?)
                ORDER BY b.booking_date, b.start_time
                """, BOOKING_ROW_MAPPER, email);
    }

    @Override
    public Optional<Booking> findByIdAndEmailIgnoreCase(Long id, String email) {
        return jdbcTemplate.query(BOOKING_COLUMNS + """
                WHERE b.id = ? AND LOWER(b.email) = LOWER(?)
                """, BOOKING_ROW_MAPPER, id, email).stream().findFirst();
    }

    @Override
    public List<Booking> findConfirmedBookings(Long venueId, LocalDate bookingDate) {
        return jdbcTemplate.query(BOOKING_COLUMNS + """
                WHERE b.venue_id = ? AND b.booking_date = ? AND b.status = ?
                ORDER BY b.start_time
                """, BOOKING_ROW_MAPPER, venueId, Date.valueOf(bookingDate), BookingStatus.CONFIRMED.name());
    }

    @Override
    public List<Booking> findAllForVenueOwner(Long ownerId) {
        return jdbcTemplate.query(BOOKING_COLUMNS + """
                WHERE v.owner_id = ?
                ORDER BY b.booking_date, b.start_time
                """, BOOKING_ROW_MAPPER, ownerId);
    }

    @Override
    public Booking insert(Booking booking) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO bookings
                    (venue_id, customer_name, phone, email, booking_date, start_time, end_time,
                     total_price, status, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, booking.getVenue().getId());
            statement.setString(2, booking.getCustomerName());
            statement.setString(3, booking.getPhone());
            statement.setString(4, booking.getEmail());
            statement.setDate(5, Date.valueOf(booking.getBookingDate()));
            statement.setTime(6, Time.valueOf(booking.getStartTime()));
            statement.setTime(7, Time.valueOf(booking.getEndTime()));
            statement.setLong(8, booking.getTotalPrice());
            statement.setString(9, booking.getStatus().name());
            statement.setTimestamp(10, Timestamp.valueOf(booking.getCreatedAt()));
            return statement;
        }, keyHolder);
        Number id = keyHolder.getKey();
        if (id == null) {
            throw new IllegalStateException("Database did not return the new booking ID.");
        }
        booking.setId(id.longValue());
        return booking;
    }

    @Override
    public void updateStatus(Booking booking) {
        jdbcTemplate.update("UPDATE bookings SET status = ? WHERE id = ?",
                booking.getStatus().name(), booking.getId());
    }
}
