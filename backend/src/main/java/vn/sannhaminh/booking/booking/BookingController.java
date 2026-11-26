package vn.sannhaminh.booking.booking;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.sannhaminh.security.AuthUser;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('PLAYER')")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public BookingCreated createBooking(@Valid @RequestBody CreateBookingRequest request,
                                        @AuthenticationPrincipal AuthUser user) {
        return bookingService.createBooking(request, user);
    }

    @GetMapping("/mine")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal AuthUser user) {
        return bookingService.listMyBookings(user);
    }

    @PatchMapping("/{id}/cancel")
    public Map<String, Object> cancelBooking(@PathVariable long id, @AuthenticationPrincipal AuthUser user) {
        bookingService.cancelBooking(id, user);
        return Map.of("success", true);
    }
}
