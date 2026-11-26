package vn.sannhaminh.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import vn.sannhaminh.booking.booking.BookingController;
import vn.sannhaminh.booking.booking.BookingCreated;
import vn.sannhaminh.booking.booking.BookingResponse;
import vn.sannhaminh.booking.booking.BookingService;
import vn.sannhaminh.booking.booking.BookingStatus;
import vn.sannhaminh.config.ApiExceptionHandler;
import vn.sannhaminh.config.SecurityConfig;
import vn.sannhaminh.security.AuthUser;
import vn.sannhaminh.security.JwtService;

@WebMvcTest(controllers = BookingController.class)
@Import({ApiExceptionHandler.class, SecurityConfig.class})
class BookingControllerTest {
    @Autowired MockMvc mvc;
    @MockBean BookingService bookingService;
    @MockBean JwtService jwtService;

    private static Authentication player() {
        AuthUser user = new AuthUser(5, "player@test.vn", "PLAYER", "Player");
        return new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
    }

    @Test void createBookingReturnsCodeAndStatus() throws Exception {
        given(bookingService.createBooking(any(), any()))
                .willReturn(new BookingCreated(9, "SNM1", BookingStatus.PENDING_OWNER, new BigDecimal("300000")));
        String date = LocalDate.now().plusDays(1).toString();
        mvc.perform(post("/api/bookings").with(authentication(player())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"venueId\":1,\"playDate\":\"" + date + "\",\"startTime\":\"18:00\",\"hours\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingCode").value("SNM1"))
                .andExpect(jsonPath("$.status").value("PENDING_OWNER"));
    }

    @Test void createBookingRejectsInvalidHours() throws Exception {
        String date = LocalDate.now().plusDays(1).toString();
        mvc.perform(post("/api/bookings").with(authentication(player())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"venueId\":1,\"playDate\":\"" + date + "\",\"startTime\":\"18:00\",\"hours\":9}"))
                .andExpect(status().isBadRequest());
    }

    @Test void myBookingsUsesSnakeCaseFields() throws Exception {
        given(bookingService.listMyBookings(any())).willReturn(List.of(new BookingResponse(1L, "SNM1", 3, "Sân A",
                "Hà Nội", null, LocalDate.now(), LocalTime.of(18, 0), LocalTime.of(19, 0), 1, new BigDecimal("150000"),
                BookingStatus.CONFIRMED, null, null)));
        mvc.perform(get("/api/bookings/mine").with(authentication(player())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].booking_code").value("SNM1"))
                .andExpect(jsonPath("$[0].venue_name").value("Sân A"));
    }

    @Test void cancelBookingReturnsSuccess() throws Exception {
        mvc.perform(patch("/api/bookings/1/cancel").with(authentication(player())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    @Test void anonymousIsRejected() throws Exception {
        mvc.perform(get("/api/bookings/mine")).andExpect(status().isUnauthorized());
    }
}
