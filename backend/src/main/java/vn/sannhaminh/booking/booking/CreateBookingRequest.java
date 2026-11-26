package vn.sannhaminh.booking.booking;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** Body của POST /api/bookings (khớp với form đặt sân ở frontend). */
public record CreateBookingRequest(
        @NotNull Long venueId,
        @NotNull @FutureOrPresent LocalDate playDate,
        @NotNull LocalTime startTime,
        @Min(1) @Max(4) Integer hours,
        @Size(max = 500) String note
) {
    public int hoursOrDefault() {
        return hours == null ? 1 : hours;
    }
}
