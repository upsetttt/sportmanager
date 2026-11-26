package vn.sannhaminh.booking.booking;

<<<<<<< HEAD
import java.math.BigDecimal;
=======
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingDao {
<<<<<<< HEAD
    record VenueInfo(long id, long ownerId, String name, BigDecimal pricePerHour) {}

    record Schedule(LocalTime openTime, LocalTime closeTime, boolean closed) {}

    /** Khóa dòng sân đang ACTIVE (SELECT ... FOR UPDATE) để tránh 2 người đặt trùng giờ. */
    Optional<VenueInfo> lockActiveVenue(long venueId);

    Optional<Schedule> findSchedule(long venueId, int dayOfWeek);

    boolean existsOverlappingBooking(long venueId, LocalDate playDate, LocalTime start, LocalTime end);

    long insert(String bookingCode, long playerId, VenueInfo venue, LocalDate playDate, LocalTime start,
                LocalTime end, int hours, BigDecimal amount, String note);

    List<Booking> findByPlayer(long playerId);

    Optional<Booking> findByIdAndPlayer(long id, long playerId);

    void updateStatus(long id, BookingStatus status);

    /** Hủy booking nguyên tử nếu trạng thái hiện tại cho phép (PENDING_OWNER hoặc CONFIRMED). */
    int cancelIfAllowed(long id, long playerId);

    /** Hủy/hoàn tiền thanh toán gắn với lịch đặt: PAID -> REFUND_PENDING, PENDING -> CANCELLED. */
    void cancelPayment(long bookingId);

    void notifyAccount(long accountId, String title, String message);

    void audit(long actorId, String action, long bookingId, String detail);
=======
    boolean existsOverlappingBooking(Long venueId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime);
    List<Booking> findByEmailIgnoreCaseOrderByBookingDateAscStartTimeAsc(String email);
    Optional<Booking> findByIdAndEmailIgnoreCase(Long id, String email);
    List<Booking> findConfirmedBookings(Long venueId, LocalDate bookingDate);
    List<Booking> findAllForVenueOwner(Long ownerId);
    Booking insert(Booking booking);
    void updateStatus(Booking booking);
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
}
