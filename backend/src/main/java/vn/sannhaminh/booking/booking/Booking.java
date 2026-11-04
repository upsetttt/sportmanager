package vn.sannhaminh.booking.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import vn.sannhaminh.booking.venue.Venue;

public class Booking {
    private Long id;
    private Venue venue;
    private String customerName;
    private String phone;
    private String email;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private long totalPrice;
    private BookingStatus status;
    private LocalDateTime createdAt;

    public Booking(Venue venue, String customerName, String phone, String email,
                   LocalDate bookingDate, LocalTime startTime, LocalTime endTime, long totalPrice) {
        this.venue = venue;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalPrice = totalPrice;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = LocalDateTime.now();
    }

    public Booking(Long id, Venue venue, String customerName, String phone, String email,
                   LocalDate bookingDate, LocalTime startTime, LocalTime endTime, long totalPrice,
                   BookingStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.venue = venue;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalPrice = totalPrice;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Venue getVenue() { return venue; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public LocalDate getBookingDate() { return bookingDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public long getTotalPrice() { return totalPrice; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
