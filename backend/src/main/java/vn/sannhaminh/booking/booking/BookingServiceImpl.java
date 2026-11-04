package vn.sannhaminh.booking.booking;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.sannhaminh.booking.account.Account;
import vn.sannhaminh.booking.account.AccountDao;
import vn.sannhaminh.booking.venue.Venue;
import vn.sannhaminh.booking.venue.VenueDao;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingDao bookingDao;
    private final VenueDao venueDao;
    private final AccountDao accountDao;

    public BookingServiceImpl(BookingDao bookingDao, VenueDao venueDao, AccountDao accountDao) {
        this.bookingDao = bookingDao;
        this.venueDao = venueDao;
        this.accountDao = accountDao;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String email) {
        Venue venue = venueDao.findByIdForUpdate(request.venueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân."));
        if (!venue.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sân hiện không nhận đặt lịch.");
        }
        Account account = findAccount(email);
        LocalTime endTime = request.startTime().plusHours(request.durationHours());
        if (!endTime.isAfter(request.startTime()) || endTime.isAfter(LocalTime.of(23, 59))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khung giờ đặt sân không hợp lệ.");
        }
        LocalDateTime bookingStartsAt = LocalDateTime.of(request.bookingDate(), request.startTime());
        if (bookingStartsAt.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đặt sân vào thời gian đã qua.");
        }
        if (bookingDao.existsOverlappingBooking(venue.getId(), request.bookingDate(), request.startTime(), endTime)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ này vừa được đặt. Vui lòng chọn giờ khác.");
        }

        Booking booking = new Booking(venue, account.getFullName(), account.getPhone(),
                account.getEmail(), request.bookingDate(), request.startTime(), endTime,
                Math.multiplyExact(venue.getPricePerHour(), request.durationHours()));
        return BookingResponse.from(bookingDao.insert(booking));
    }

    @Override
    public List<BookingResponse> listBookings(String email) {
        return bookingDao.findByEmailIgnoreCaseOrderByBookingDateAscStartTimeAsc(email)
                .stream().map(BookingResponse::from).toList();
    }

    @Override
    public List<BookingResponse> listBookingsForMyVenues(String email) {
        Account owner = findAccount(email);
        return bookingDao.findAllForVenueOwner(owner.getId())
                .stream().map(BookingResponse::from).toList();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long id, String email) {
        Booking booking = bookingDao.findByIdAndEmailIgnoreCase(id, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đặt."));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lịch đặt này đã được hủy.");
        }
        if (!LocalDateTime.of(booking.getBookingDate(), booking.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể hủy lịch đã bắt đầu.");
        }
        booking.cancel();
        bookingDao.updateStatus(booking);
        return BookingResponse.from(booking);
    }

    private Account findAccount(String email) {
        return accountDao.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản không còn hợp lệ."));
    }
}
