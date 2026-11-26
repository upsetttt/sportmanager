package vn.sannhaminh.booking.booking;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
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
            FROM bookings b
            JOIN venues v ON v.id = b.venue_id
            """;

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
        return count != null && count > 0;
    }

    @Override
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
    }
}
