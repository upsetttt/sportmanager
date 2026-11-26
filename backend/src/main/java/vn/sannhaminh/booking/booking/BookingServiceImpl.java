package vn.sannhaminh.booking.booking;

<<<<<<< HEAD
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
=======
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
<<<<<<< HEAD
import vn.sannhaminh.security.AuthUser;
=======
import vn.sannhaminh.booking.account.Account;
import vn.sannhaminh.booking.account.AccountDao;
import vn.sannhaminh.booking.venue.Venue;
import vn.sannhaminh.booking.venue.VenueDao;
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingDao bookingDao;
<<<<<<< HEAD

    public BookingServiceImpl(BookingDao bookingDao) {
        this.bookingDao = bookingDao;
=======
    private final VenueDao venueDao;
    private final AccountDao accountDao;

    public BookingServiceImpl(BookingDao bookingDao, VenueDao venueDao, AccountDao accountDao) {
        this.bookingDao = bookingDao;
        this.venueDao = venueDao;
        this.accountDao = accountDao;
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
    }

    @Override
    @Transactional
<<<<<<< HEAD
    public BookingCreated createBooking(CreateBookingRequest request, AuthUser player) {
        int hours = request.hoursOrDefault();
        LocalDate date = request.playDate();
        LocalTime start = request.startTime();

        var venue = bookingDao.lockActiveVenue(request.venueId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sân hoặc sân chưa nhận đặt lịch."));

        LocalTime end = start.plusHours(hours);
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khung giờ không được vượt qua ngày kế tiếp");
        }
        if (LocalDateTime.of(date, start).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể đặt khung giờ đã qua");
        }
        bookingDao.findSchedule(venue.id(), date.getDayOfWeek().getValue()).ifPresent(s -> {
            if (s.closed() || start.isBefore(s.openTime()) || end.isAfter(s.closeTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Khung giờ nằm ngoài lịch hoạt động của sân");
            }
        });
        if (bookingDao.existsOverlappingBooking(venue.id(), date, start, end)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Khung giờ này đã có người đặt");
        }

        BigDecimal amount = venue.pricePerHour().multiply(BigDecimal.valueOf(hours));
        String code = "SNM" + System.currentTimeMillis() + String.format("%04X", ThreadLocalRandom.current().nextInt(0x10000));
        long id = bookingDao.insert(code, player.id(), venue, date, start, end, hours, amount, request.note());

        bookingDao.notifyAccount(venue.ownerId(), "Có yêu cầu đặt sân mới",
                "Khách vừa gửi yêu cầu đặt sân " + venue.name() + ". Vui lòng xác nhận hoặc từ chối.");
        bookingDao.audit(player.id(), "BOOKING_CREATE", id, code);
        return new BookingCreated(id, code, BookingStatus.PENDING_OWNER, amount);
    }

    @Override
    public List<BookingResponse> listMyBookings(AuthUser player) {
        return bookingDao.findByPlayer(player.id()).stream().map(BookingResponse::from).toList();
=======
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
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
    }

    @Override
    @Transactional
<<<<<<< HEAD
    public void cancelBooking(long id, AuthUser player) {
        Booking booking = bookingDao.findByIdAndPlayer(id, player.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đặt."));
        if (!booking.canBeCancelled() || bookingDao.cancelIfAllowed(id, player.id()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hủy lịch này");
        }
        bookingDao.cancelPayment(id);
        bookingDao.notifyAccount(booking.ownerId(), "Lịch đặt đã bị hủy", "Một lịch đặt sân đã được khách hủy.");
        bookingDao.audit(player.id(), "BOOKING_CANCEL", id, null);
=======
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
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
    }
}
