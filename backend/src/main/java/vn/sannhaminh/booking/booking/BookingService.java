package vn.sannhaminh.booking.booking;

import java.util.List;
import vn.sannhaminh.security.AuthUser;

public interface BookingService {
    BookingCreated createBooking(CreateBookingRequest request, AuthUser player);

    List<BookingResponse> listMyBookings(AuthUser player);

    void cancelBooking(long id, AuthUser player);
}
