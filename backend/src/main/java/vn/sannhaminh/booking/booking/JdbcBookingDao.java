package vn.sannhaminh.booking.booking;

<<<<<<< HEAD
import java.math.BigDecimal;
import java.sql.Statement;
=======
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
<<<<<<< HEAD
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcBookingDao implements BookingDao {
    private static final String SELECT_BOOKING = """
            SELECT b.id, b.booking_code, b.player_id, b.owner_id, b.venue_id, v.name AS venue_name,
                   v.address, v.image_url, b.play_date, b.start_time, b.end_time, b.total_hours,
                   b.total_amount, b.status, b.customer_note, b.created_at
=======
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
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
            FROM bookings b
            JOIN venues v ON v.id = b.venue_id
            """;

<<<<<<< HEAD
    private static final RowMapper<Booking> BOOKING_MAPPER = (rs, i) -> new Booking(
            rs.getLong("id"),
            rs.getString("booking_code"),
            rs.getLong("player_id"),
            rs.getLong("owner_id"),
            rs.getLong("venue_id"),
            rs.getString("venue_name"),
            rs.getString("address"),
            rs.getString("image_url"),
            rs.getDate("play_date").toLocalDate(),
            rs.getTime("start_time").toLocalTime(),
            rs.getTime("end_time").toLocalTime(),
            rs.getInt("total_hours"),
            rs.getBigDecimal("total_amount"),
            BookingStatus.valueOf(rs.getString("status")),
            rs.getString("customer_note"),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final JdbcTemplate db;

    public JdbcBookingDao(JdbcTemplate db) {
        this.db = db;
    }

    @Override
    public Optional<VenueInfo> lockActiveVenue(long venueId) {
        return db.query("""
                        SELECT v.id, v.owner_id, v.name, v.price_per_hour
                        FROM venues v
                        JOIN accounts a ON a.id = v.owner_id
                        WHERE v.id=? AND v.status='ACTIVE' AND a.status='ACTIVE'
                        FOR UPDATE
                        """,
                (rs, i) -> new VenueInfo(rs.getLong("id"), rs.getLong("owner_id"), rs.getString("name"),
                        rs.getBigDecimal("price_per_hour")),
                venueId).stream().findFirst();
    }

    @Override
    public Optional<Schedule> findSchedule(long venueId, int dayOfWeek) {
        return db.query("SELECT open_time, close_time, is_closed FROM venue_schedules WHERE venue_id=? AND day_of_week=?",
                (rs, i) -> new Schedule(rs.getTime("open_time").toLocalTime(), rs.getTime("close_time").toLocalTime(),
                        rs.getBoolean("is_closed")),
                venueId, dayOfWeek).stream().findFirst();
    }

    @Override
    public boolean existsOverlappingBooking(long venueId, LocalDate playDate, LocalTime start, LocalTime end) {
        Integer count = db.queryForObject("""
                SELECT COUNT(*) FROM bookings
                WHERE venue_id=? AND play_date=? AND status IN ('PENDING_OWNER','CONFIRMED')
                  AND start_time < ? AND end_time > ?
                """, Integer.class, venueId, playDate, end, start);
=======
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
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
        return count != null && count > 0;
    }

    @Override
<<<<<<< HEAD
    public long insert(String bookingCode, long playerId, VenueInfo venue, LocalDate playDate, LocalTime start,
                       LocalTime end, int hours, BigDecimal amount, String note) {
        var keys = new GeneratedKeyHolder();
        db.update(con -> {
            var ps = con.prepareStatement("""
                    INSERT INTO bookings(booking_code, player_id, owner_id, venue_id, play_date, start_time, end_time,
                                         total_hours, total_amount, status, customer_note)
                    VALUES (?,?,?,?,?,?,?,?,?,'PENDING_OWNER',?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, bookingCode);
            ps.setLong(2, playerId);
            ps.setLong(3, venue.ownerId());
            ps.setLong(4, venue.id());
            ps.setObject(5, playDate);
            ps.setObject(6, start);
            ps.setObject(7, end);
            ps.setInt(8, hours);
            ps.setBigDecimal(9, amount);
            ps.setString(10, note);
            return ps;
        }, keys);
        Number id = keys.getKey();
        if (id == null) {
            throw new IllegalStateException("Database did not return the new booking ID.");
        }
        return id.longValue();
    }

    @Override
    public List<Booking> findByPlayer(long playerId) {
        return db.query(SELECT_BOOKING + " WHERE b.player_id=? ORDER BY b.created_at DESC", BOOKING_MAPPER, playerId);
    }

    @Override
    public Optional<Booking> findByIdAndPlayer(long id, long playerId) {
        try {
            return Optional.ofNullable(db.queryForObject(SELECT_BOOKING + " WHERE b.id=? AND b.player_id=?",
                    BOOKING_MAPPER, id, playerId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void updateStatus(long id, BookingStatus status) {
        db.update("UPDATE bookings SET status=? WHERE id=?", status.name(), id);
    }

    @Override
    public int cancelIfAllowed(long id, long playerId) {
        return db.update(
                "UPDATE bookings SET status='CANCELLED' WHERE id=? AND player_id=? AND status IN ('PENDING_OWNER','CONFIRMED')",
                id, playerId);
    }

    @Override
    public void cancelPayment(long bookingId) {
        db.update("UPDATE payments SET status=CASE WHEN status='PAID' THEN 'REFUND_PENDING' ELSE 'CANCELLED' END WHERE booking_id=? AND status IN ('PENDING','PAID')",
                bookingId);
    }

    @Override
    public void notifyAccount(long accountId, String title, String message) {
        db.update("INSERT INTO notifications(account_id,title,message) VALUES(?,?,?)", accountId, title, message);
    }

    @Override
    public void audit(long actorId, String action, long bookingId, String detail) {
        db.update("INSERT INTO audit_logs(actor_id,action,target_type,target_id,details) VALUES(?,?,?,?,?)",
                actorId, action, "BOOKING", bookingId, detail);
=======
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
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
    }
}
