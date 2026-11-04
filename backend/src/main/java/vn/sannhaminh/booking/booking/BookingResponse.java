package vn.sannhaminh.booking.booking;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
        Long id,
        String venueName,
        String customerName,
        String email,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        long totalPrice,
        BookingStatus status
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(), booking.getVenue().getName(), booking.getCustomerName(),
                booking.getEmail(), booking.getBookingDate(), booking.getStartTime(), booking.getEndTime(),
                booking.getTotalPrice(), booking.getStatus());
    }
}
