package vn.sannhaminh.booking.booking;

import java.util.List;
<<<<<<< HEAD
import vn.sannhaminh.security.AuthUser;

public interface BookingService {
    BookingCreated createBooking(CreateBookingRequest request, AuthUser player);

    List<BookingResponse> listMyBookings(AuthUser player);

    void cancelBooking(long id, AuthUser player);
=======

public interface BookingService {
    BookingResponse createBooking(CreateBookingRequest request, String email);
    List<BookingResponse> listBookings(String email);
    List<BookingResponse> listBookingsForMyVenues(String email);
    BookingResponse cancelBooking(Long id, String email);
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
}
