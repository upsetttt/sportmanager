package vn.sannhaminh.booking.booking;

import jakarta.validation.Valid;
import java.util.List;
<<<<<<< HEAD
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
=======
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
<<<<<<< HEAD
import vn.sannhaminh.security.AuthUser;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasRole('PLAYER')")
=======

@RestController
@RequestMapping("/api/bookings")
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
<<<<<<< HEAD
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
=======
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
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
    }
}
