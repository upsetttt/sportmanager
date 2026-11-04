package vn.sannhaminh.booking.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingDao {
    boolean existsOverlappingBooking(Long venueId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime);
    List<Booking> findByEmailIgnoreCaseOrderByBookingDateAscStartTimeAsc(String email);
    Optional<Booking> findByIdAndEmailIgnoreCase(Long id, String email);
    List<Booking> findConfirmedBookings(Long venueId, LocalDate bookingDate);
    List<Booking> findAllForVenueOwner(Long ownerId);
    Booking insert(Booking booking);
    void updateStatus(Booking booking);
}
