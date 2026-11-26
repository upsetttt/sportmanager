package vn.sannhaminh.booking.venue;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.sannhaminh.booking.booking.BookedSlotResponse;

@RestController
@RequestMapping("/api/venues")
@Validated
public class VenueController {
    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public List<VenueResponse> listVenues() {
        return venueService.listVenues();
    }

    @GetMapping("/{id}/availability")
    public List<BookedSlotResponse> listBookedSlots(@PathVariable Long id, @RequestParam LocalDate date) {
        return venueService.listBookedSlots(id, date);
    }

    @GetMapping("/mine")
    public List<VenueResponse> listMyVenues(Authentication authentication) {
        return venueService.listMyVenues(authentication.getName());
    }

    @PostMapping
    public VenueResponse createVenue(@Valid @RequestBody VenueRequest request, Authentication authentication) {
        return venueService.createVenue(request, authentication.getName());
    }

    @PutMapping("/{id}")
    public VenueResponse updateVenue(@PathVariable Long id, @Valid @RequestBody VenueRequest request,
                                     Authentication authentication) {
        return venueService.updateVenue(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public void archiveVenue(@PathVariable Long id, Authentication authentication) {
        venueService.archiveVenue(id, authentication.getName());
    }
}
