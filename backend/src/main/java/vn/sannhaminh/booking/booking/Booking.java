package vn.sannhaminh.booking.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record Booking(
        Long id,
        String bookingCode,
        long playerId,
        long ownerId,
        long venueId,
        String venueName,
        String address,
        String imageUrl,
        LocalDate playDate,
        LocalTime startTime,
        LocalTime endTime,
        int totalHours,
        BigDecimal totalAmount,
        BookingStatus status,
        String customerNote,
        LocalDateTime createdAt
) {
    public boolean canBeCancelled() {
        return status == BookingStatus.PENDING_OWNER || status == BookingStatus.CONFIRMED;
    }
}
