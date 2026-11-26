package vn.sannhaminh.booking.booking;

import java.time.LocalTime;

public record BookedSlotResponse(LocalTime startTime, LocalTime endTime) {
    public static BookedSlotResponse from(Booking booking) {
        return new BookedSlotResponse(booking.getStartTime(), booking.getEndTime());
    }
}
