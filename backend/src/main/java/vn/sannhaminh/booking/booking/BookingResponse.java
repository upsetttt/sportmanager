package vn.sannhaminh.booking.booking;

<<<<<<< HEAD
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Giữ tên field dạng snake_case để frontend hiện tại (booking_code, play_date, ...) dùng được ngay. */
public record BookingResponse(
        Long id,
        @JsonProperty("booking_code") String bookingCode,
        @JsonProperty("venue_id") long venueId,
        @JsonProperty("venue_name") String venueName,
        String address,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("play_date") LocalDate playDate,
        @JsonProperty("start_time") LocalTime startTime,
        @JsonProperty("end_time") LocalTime endTime,
        @JsonProperty("total_hours") int totalHours,
        @JsonProperty("total_amount") BigDecimal totalAmount,
        BookingStatus status,
        @JsonProperty("customer_note") String customerNote,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.id(), b.bookingCode(), b.venueId(), b.venueName(), b.address(), b.imageUrl(),
                b.playDate(), b.startTime(), b.endTime(), b.totalHours(), b.totalAmount(), b.status(),
                b.customerNote(), b.createdAt());
=======
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
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
    }
}
