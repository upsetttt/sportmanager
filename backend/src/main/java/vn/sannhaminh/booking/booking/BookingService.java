package vn.sannhaminh.booking.booking;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(CreateBookingRequest request, String email);
    List<BookingResponse> listBookings(String email);
    List<BookingResponse> listBookingsForMyVenues(String email);
    BookingResponse cancelBooking(Long id, String email);
}
