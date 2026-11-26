package vn.sannhaminh.booking.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.sannhaminh.security.AuthUser;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingDao bookingDao;

    public BookingServiceImpl(BookingDao bookingDao) {
        this.bookingDao = bookingDao;
    }

    @Override
    @Transactional
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
    }

    @Override
    @Transactional
    public void cancelBooking(long id, AuthUser player) {
        Booking booking = bookingDao.findByIdAndPlayer(id, player.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lịch đặt."));
        if (!booking.canBeCancelled() || bookingDao.cancelIfAllowed(id, player.id()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hủy lịch này");
        }
        bookingDao.cancelPayment(id);
        bookingDao.notifyAccount(booking.ownerId(), "Lịch đặt đã bị hủy", "Một lịch đặt sân đã được khách hủy.");
        bookingDao.audit(player.id(), "BOOKING_CANCEL", id, null);
    }
}
