package vn.sannhaminh.booking.booking;

import java.math.BigDecimal;

/** Kết quả trả về sau khi đặt sân (frontend dùng id, bookingCode, status, totalAmount). */
public record BookingCreated(long id, String bookingCode, BookingStatus status, BigDecimal totalAmount) {}
