package vn.sannhaminh.booking.venue;

import java.time.LocalDate;
import java.util.List;
import vn.sannhaminh.booking.booking.BookedSlotResponse;

public interface VenueService {
    List<VenueResponse> listVenues();
    List<BookedSlotResponse> listBookedSlots(Long id, LocalDate date);
    List<VenueResponse> listMyVenues(String email);
    VenueResponse createVenue(VenueRequest request, String email);
    VenueResponse updateVenue(Long id, VenueRequest request, String email);
    void archiveVenue(Long id, String email);
}
