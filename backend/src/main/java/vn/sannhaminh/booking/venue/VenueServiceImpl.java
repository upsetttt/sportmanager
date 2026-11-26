package vn.sannhaminh.booking.venue;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.sannhaminh.booking.account.Account;
import vn.sannhaminh.booking.account.AccountDao;
import vn.sannhaminh.booking.booking.BookedSlotResponse;
import vn.sannhaminh.booking.booking.BookingDao;

@Service
public class VenueServiceImpl implements VenueService {
    private final VenueDao venueDao;
    private final AccountDao accountDao;
    private final BookingDao bookingDao;

    public VenueServiceImpl(VenueDao venueDao, AccountDao accountDao, BookingDao bookingDao) {
        this.venueDao = venueDao;
        this.accountDao = accountDao;
        this.bookingDao = bookingDao;
    }

    @Override
    public List<VenueResponse> listVenues() {
        return venueDao.findAllActive().stream().map(VenueResponse::from).toList();
    }

    @Override
    public List<BookedSlotResponse> listBookedSlots(Long id, LocalDate date) {
        Venue venue = venueDao.findById(id)
                .filter(Venue::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân."));
        return bookingDao.findConfirmedBookings(venue.getId(), date).stream()
                .map(BookedSlotResponse::from).toList();
    }

    @Override
    public List<VenueResponse> listMyVenues(String email) {
        Account owner = findOwner(email);
        return venueDao.findAllActiveByOwnerId(owner.getId())
                .stream().map(VenueResponse::from).toList();
    }

    @Override
    @Transactional
    public VenueResponse createVenue(VenueRequest request, String email) {
        Account owner = findOwner(email);
        Venue venue = new Venue(request.name().trim(), request.sport().trim(), request.address().trim(),
                request.pricePerHour(), blankToNull(request.imageUrl()), joinAmenities(request.amenities()), owner);
        return VenueResponse.from(venueDao.insert(venue));
    }

    @Override
    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest request, String email) {
        Venue venue = findOwnedVenue(id, email);
        venue.update(request.name().trim(), request.sport().trim(), request.address().trim(),
                request.pricePerHour(), blankToNull(request.imageUrl()), joinAmenities(request.amenities()));
        venueDao.update(venue);
        return VenueResponse.from(venue);
    }

    @Override
    @Transactional
    public void archiveVenue(Long id, String email) {
        findOwnedVenue(id, email).archive();
        venueDao.archive(id);
    }

    private Account findOwner(String email) {
        return accountDao.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn hợp lệ."));
    }

    private Venue findOwnedVenue(Long id, String email) {
        Account owner = findOwner(email);
        Venue venue = venueDao.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân."));
        if (venue.getOwner() == null || !venue.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân.");
        }
        return venue;
    }

    private String joinAmenities(List<String> amenities) {
        if (amenities == null) return "";
        return amenities.stream().map(String::trim).filter(value -> !value.isBlank())
                .distinct().collect(Collectors.joining("|"));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
