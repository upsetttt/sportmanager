package vn.sannhaminh.booking.booking;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request, Authentication authentication) {
        return bookingService.createBooking(request, authentication.getName());
    }

    @GetMapping
    public List<BookingResponse> listBookings(Authentication authentication) {
        return bookingService.listBookings(authentication.getName());
    }

    @GetMapping("/managed")
    public List<BookingResponse> listBookingsForMyVenues(Authentication authentication) {
        return bookingService.listBookingsForMyVenues(authentication.getName());
    }

    @DeleteMapping("/{id}")
    public BookingResponse cancelBooking(@PathVariable Long id, Authentication authentication) {
        return bookingService.cancelBooking(id, authentication.getName());
    }
}
