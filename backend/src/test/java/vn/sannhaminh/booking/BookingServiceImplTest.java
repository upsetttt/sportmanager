package vn.sannhaminh.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.sannhaminh.booking.booking.Booking;
import vn.sannhaminh.booking.booking.BookingCreated;
import vn.sannhaminh.booking.booking.BookingDao;
import vn.sannhaminh.booking.booking.BookingServiceImpl;
import vn.sannhaminh.booking.booking.BookingStatus;
import vn.sannhaminh.booking.booking.CreateBookingRequest;
import vn.sannhaminh.security.AuthUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {
    @Mock BookingDao bookingDao;
    BookingServiceImpl service;
    AuthUser player = new AuthUser(5L, "player@test.vn", "PLAYER", "Player");

    @BeforeEach
    void setUp() {
        service = new BookingServiceImpl(bookingDao);
    }

    @Test
    void createBookingGeneratesUniqueCodeWithinColumnLimit() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        given(bookingDao.lockActiveVenue(1L))
                .willReturn(Optional.of(new BookingDao.VenueInfo(1L, 2L, "Sân A", new BigDecimal("200000"))));
        given(bookingDao.findSchedule(1L, tomorrow.getDayOfWeek().getValue()))
                .willReturn(Optional.of(new BookingDao.Schedule(LocalTime.of(6, 0), LocalTime.of(22, 0), false)));
        given(bookingDao.existsOverlappingBooking(1L, tomorrow, LocalTime.of(18, 0), LocalTime.of(20, 0))).willReturn(false);
        given(bookingDao.insert(anyString(), eq(5L), any(), eq(tomorrow), eq(LocalTime.of(18, 0)), eq(LocalTime.of(20, 0)),
                eq(2), eq(new BigDecimal("400000")), isNull())).willReturn(99L);

        BookingCreated created = service.createBooking(
                new CreateBookingRequest(1L, tomorrow, LocalTime.of(18, 0), 2, null), player);

        assertThat(created.id()).isEqualTo(99L);
        assertThat(created.bookingCode()).startsWith("SNM").hasSizeLessThanOrEqualTo(30);
        assertThat(created.totalAmount()).isEqualByComparingTo("400000");
    }

    @Test
    void cancelBookingUsesAtomicCancelAndRejectsConcurrentStateChange() {
        Booking confirmed = new Booking(10L, "SNM10", 5L, 2L, 1L, "Sân A", "Hà Nội", null,
                LocalDate.now().plusDays(1), LocalTime.of(18, 0), LocalTime.of(19, 0), 1,
                new BigDecimal("200000"), BookingStatus.CONFIRMED, null, LocalDateTime.now());
        given(bookingDao.findByIdAndPlayer(10L, 5L)).willReturn(Optional.of(confirmed));
        given(bookingDao.cancelIfAllowed(10L, 5L)).willReturn(0);

        assertThatThrownBy(() -> service.cancelBooking(10L, player))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(bookingDao, never()).cancelPayment(10L);
    }
}
