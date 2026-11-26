package vn.sannhaminh.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.sannhaminh.config.ApiExceptionHandler;
import vn.sannhaminh.config.SecurityConfig;
import vn.sannhaminh.security.AuthUser;
import vn.sannhaminh.security.JwtService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiController.class)
@Import({ApiExceptionHandler.class, SecurityConfig.class})
class ApiControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean JdbcTemplate db;
    @MockBean JwtService jwtService;

    private static Authentication authAs(long id, String role) {
        AuthUser user = new AuthUser(id, role.toLowerCase() + "@test.vn", role, role);
        return new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test void healthEndpointReturnsUp() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void categoriesEndpointIsPublicAndReturnsDatabaseRows() throws Exception {
        given(db.queryForList(contains("FROM categories"))).willReturn(List.of(Map.of("id", 1, "name", "Bóng đá")));
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Bóng đá"));
    }

    @Test void publicVenueSearchSupportsTextAndFilter() throws Exception {
        given(db.queryForList(contains("FROM venues v"), any(Object[].class))).willReturn(List.of(Map.of("id", 7, "name", "Sân Cầu Giấy")));
        mvc.perform(get("/api/venues").param("q", "Cầu Giấy").param("district", "Cầu Giấy").param("sort", "price_asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
    }

    @Test void paginatedVenueSearchReturnsPageMetadata() throws Exception {
        given(db.queryForObject(startsWith("SELECT COUNT(*)"), eq(Integer.class), any(Object[].class))).willReturn(25);
        given(db.queryForList(startsWith("SELECT v.*"), any(Object[].class))).willReturn(List.of(Map.of("id", 1, "name", "Sân A")));
        mvc.perform(get("/api/venues/page").param("page", "1").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(25)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Sân A"));
    }

    @Test void adminApiRejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/admin/accounts")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "PLAYER") void adminApiRejectsPlayerRole() throws Exception {
        mvc.perform(get("/api/admin/accounts")).andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN") void adminApiAllowsAdminRole() throws Exception {
        given(db.queryForList(contains("FROM accounts"))).willReturn(List.of(Map.of("id", 1, "email", "admin@test.local")));
        mvc.perform(get("/api/admin/accounts")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@test.local"));
    }

    @Test void paginationRejectsInvalidPageSize() throws Exception {
        mvc.perform(get("/api/venues/page").param("page", "-1").param("size", "100"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("page phải lớn hơn hoặc bằng 0"));
    }

    @Test void adminCreateAccountEncodesPasswordAndReturnsId() throws Exception {
        given(db.queryForObject(contains("FROM accounts WHERE email=?"), eq(Integer.class), eq("new@test.vn"))).willReturn(0);
        given(db.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).willAnswer(inv -> {
            KeyHolder kh = inv.getArgument(1);
            kh.getKeyList().add(Map.of("GENERATED_KEY", 42L));
            return 1;
        });
        mvc.perform(post("/api/admin/accounts").with(authentication(authAs(1, "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"User Moi\",\"email\":\"new@test.vn\",\"password\":\"Secret123!\",\"role\":\"OWNER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test void adminResetPasswordWorksWithoutBeanInstantiationFailure() throws Exception {
        given(db.update(contains("SET password_hash=?"), anyString(), eq(5L))).willReturn(1);
        mvc.perform(patch("/api/admin/accounts/5/password").with(authentication(authAs(1, "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"NewPass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test void createPaymentRejectsOverwritingAlreadyPaidPayment() throws Exception {
        given(db.queryForList(contains("FROM bookings WHERE id=? AND player_id=?"), eq(10L), eq(5L)))
                .willReturn(List.of(Map.of("id", 10L, "status", "CONFIRMED", "total_amount", new BigDecimal("300000"))));
        given(db.queryForList(contains("FROM payments WHERE booking_id=?"), eq(10L)))
                .willReturn(List.of(Map.of("id", 77L, "status", "PAID")));

        mvc.perform(post("/api/payments").with(authentication(authAs(5, "PLAYER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":10,\"method\":\"CASH\"}"))
                .andExpect(status().isConflict());
    }

    @Test void adminCancelBookingDoesNotMutatePaymentsWhenBookingAlreadyCompleted() throws Exception {
        given(db.queryForList(contains("FROM bookings WHERE id=?"), eq(15L)))
                .willReturn(List.of(Map.of("id", 15L, "player_id", 5L, "owner_id", 2L, "booking_code", "SNM15", "status", "COMPLETED")));
        given(db.update(contains("UPDATE bookings SET status='CANCELLED'"), eq(15L))).willReturn(0);

        mvc.perform(patch("/api/admin/bookings/15/cancel").with(authentication(authAs(1, "ADMIN"))))
                .andExpect(status().isConflict());
        verify(db, never()).update(contains("UPDATE payments"), eq(15L));
    }

    @Test void saveSchedulesRejectsCloseTimeBeforeOpenTime() throws Exception {
        given(db.queryForObject(contains("FROM venues WHERE id=? AND owner_id=?"), eq(Integer.class), eq(3L), eq(2L))).willReturn(1);
        mvc.perform(put("/api/owner/venues/3/schedules").with(authentication(authAs(2, "OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"dayOfWeek\":1,\"openTime\":\"20:00\",\"closeTime\":\"08:00\",\"slotMinutes\":60,\"isClosed\":false}]"))
                .andExpect(status().isBadRequest());
    }

    @Test void ownerCanAcceptPendingBookingRequest() throws Exception {
        given(db.queryForList(contains("FROM bookings b JOIN venues v"), eq(21L), eq(101L), eq(101L)))
                .willReturn(List.of(Map.of("id", 21L, "player_id", 103L, "venue_id", 1L,
                        "play_date", "2026-12-01", "start_time", "18:00:00", "end_time", "19:00:00",
                        "status", "PENDING_OWNER")));
        given(db.queryForObject(contains("status='CONFIRMED' AND start_time < ? AND end_time > ?"),
                eq(Integer.class), eq(1L), eq("2026-12-01"), eq(21L), eq("19:00:00"), eq("18:00:00")))
                .willReturn(0);
        given(db.update(contains("UPDATE bookings SET status=?, owner_note=?, owner_id=?"),
                eq("CONFIRMED"), eq(""), eq(101L), eq(21L))).willReturn(1);

        mvc.perform(patch("/api/owner/bookings/21/decision").with(authentication(authAs(101, "OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(db).update(contains("INSERT INTO notifications"), eq(103L), anyString(), anyString());
        verify(db).update(contains("INSERT INTO audit_logs"), eq(101L), eq("BOOKING_CONFIRMED"), eq("BOOKING"), eq(21L), isNull());
    }

    @Test void ownerCanRejectPendingBookingRequestWithNote() throws Exception {
        given(db.queryForList(contains("FROM bookings b JOIN venues v"), eq(22L), eq(101L), eq(101L)))
                .willReturn(List.of(Map.of("id", 22L, "player_id", 103L, "venue_id", 1L,
                        "play_date", "2026-12-01", "start_time", "19:00:00", "end_time", "20:00:00",
                        "status", "PENDING_OWNER")));
        given(db.update(contains("UPDATE bookings SET status=?, owner_note=?, owner_id=?"),
                eq("REJECTED"), eq("Sân bảo trì"), eq(101L), eq(22L))).willReturn(1);

        mvc.perform(patch("/api/owner/bookings/22/decision").with(authentication(authAs(101, "OWNER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":false,\"note\":\"Sân bảo trì\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value(22))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test void decideBookingRejectsNonOwnerRolesAndAnonymous() throws Exception {
        mvc.perform(patch("/api/owner/bookings/21/decision")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accept\":true}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/owner/bookings/21/decision").with(authentication(authAs(103, "PLAYER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accept\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/owner/bookings/21/decision").with(authentication(authAs(1, "ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accept\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test void decideBookingRejectsAlreadyConfirmedOrOverlappingSlot() throws Exception {
        given(db.queryForList(contains("FROM bookings b JOIN venues v"), eq(23L), eq(101L), eq(101L)))
                .willReturn(List.of(Map.of("id", 23L, "player_id", 103L, "venue_id", 1L,
                        "play_date", "2026-12-01", "start_time", "18:00:00", "end_time", "19:00:00",
                        "status", "CONFIRMED")));

        mvc.perform(patch("/api/owner/bookings/23/decision").with(authentication(authAs(101, "OWNER")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"accept\":true}"))
                .andExpect(status().isConflict());
    }

    @Test void adminCanReadAndDeleteUserAccount() throws Exception {
        given(db.queryForList(contains("FROM accounts WHERE id=?"), eq(103L)))
                .willReturn(List.of(Map.of("id", 103L, "full_name", "Player A", "email", "p@test.vn", "role", "PLAYER", "status", "ACTIVE")));
        given(db.queryForObject(contains("FROM venues WHERE owner_id=?"), eq(Integer.class), eq(103L))).willReturn(0);
        given(db.queryForObject(contains("FROM bookings WHERE player_id=?"), eq(Integer.class), eq(103L))).willReturn(3);

        mvc.perform(get("/api/admin/accounts/103").with(authentication(authAs(1, "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("p@test.vn"))
                .andExpect(jsonPath("$.booking_count").value(3));

        given(db.queryForList(contains("SELECT role, status FROM accounts WHERE id=?"), eq(103L)))
                .willReturn(List.of(Map.of("role", "PLAYER", "status", "ACTIVE")));
        mvc.perform(delete("/api/admin/accounts/103").with(authentication(authAs(1, "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED"));
    }

    @Test void adminCannotDeleteAdminOrSelf() throws Exception {
        mvc.perform(delete("/api/admin/accounts/1").with(authentication(authAs(1, "ADMIN"))))
                .andExpect(status().isConflict());

        given(db.queryForList(contains("SELECT role, status FROM accounts WHERE id=?"), eq(2L)))
                .willReturn(List.of(Map.of("role", "ADMIN", "status", "ACTIVE")));
        mvc.perform(delete("/api/admin/accounts/2").with(authentication(authAs(1, "ADMIN"))))
                .andExpect(status().isConflict());
    }

    @Test void adminAndOwnerPitchCrudDetailAndDeleteWork() throws Exception {
        given(db.queryForList(contains("FROM venues v JOIN accounts a ON a.id=v.owner_id"), eq(10L)))
                .willReturn(List.of(Map.of("id", 10L, "name", "Sân bóng đá 7 người", "category_name", "Bóng đá", "status", "ACTIVE")));
        given(db.queryForList(contains("FROM venue_schedules WHERE venue_id=?"), eq(10L)))
                .willReturn(List.of());

        mvc.perform(get("/api/admin/venues/10").with(authentication(authAs(1, "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sân bóng đá 7 người"));

        given(db.update(contains("UPDATE venues SET status='ARCHIVED' WHERE id=? AND status<>'ARCHIVED'"), eq(10L)))
                .willReturn(1);
        mvc.perform(delete("/api/admin/venues/10").with(authentication(authAs(1, "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        given(db.queryForList(contains("WHERE v.id=? AND v.owner_id=?"), eq(10L), eq(101L)))
                .willReturn(List.of(Map.of("id", 10L, "name", "Sân bóng đá 7 người", "status", "ACTIVE")));
        mvc.perform(get("/api/owner/venues/10").with(authentication(authAs(101, "OWNER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }
}
