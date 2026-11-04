package vn.sannhaminh.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingApiIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registrationRejectsInvalidEmailAndPhone() {
        ResponseEntity<String> response = restTemplate.postForEntity("/api/auth/register", Map.of(
                "fullName", "Invalid User",
                "email", "not-an-email",
                "phone", "invalid-phone",
                "password", "test-password-123"
        ), String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void ownerCanManageVenuesAndPlayerCanBookAndCancelWithoutLeakingOtherAccounts() {
        String ownerToken = login("test-owner@example.test", "test-owner-password");
        HttpHeaders ownerHeaders = bearer(ownerToken);

        ResponseEntity<Map> venueResponse = restTemplate.exchange("/api/venues", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Sân kiểm thử",
                        "sport", "Tennis",
                        "address", "Cầu Giấy, Hà Nội",
                        "pricePerHour", 180000,
                        "amenities", List.of("Có đèn")
                ), ownerHeaders), Map.class);
        assertEquals(HttpStatus.OK, venueResponse.getStatusCode());
        Long venueId = ((Number) venueResponse.getBody().get("id")).longValue();
        assertNotNull(venueId);

        ResponseEntity<Map> updatedVenue = restTemplate.exchange("/api/venues/" + venueId, HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "name", "Sân kiểm thử đã cập nhật",
                        "sport", "Tennis",
                        "address", "Cầu Giấy, Hà Nội",
                        "pricePerHour", 180000,
                        "amenities", List.of("Có đèn")
                ), ownerHeaders), Map.class);
        assertEquals(HttpStatus.OK, updatedVenue.getStatusCode());
        assertEquals("Sân kiểm thử đã cập nhật", updatedVenue.getBody().get("name"));

        ResponseEntity<List<Map<String, Object>>> ownedVenues = restTemplate.exchange("/api/venues/mine",
                HttpMethod.GET, new HttpEntity<>(ownerHeaders), new ParameterizedTypeReference<>() {});
        assertEquals(5, ownedVenues.getBody().size());

        String playerToken = register("player");
        HttpHeaders playerHeaders = bearer(playerToken);
        ResponseEntity<Map> playerCannotCreateVenue = restTemplate.exchange("/api/venues", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Sân không được phép",
                        "sport", "Tennis",
                        "address", "Hà Nội",
                        "pricePerHour", 180000,
                        "amenities", List.of()
                ), playerHeaders), Map.class);
        assertEquals(HttpStatus.FORBIDDEN, playerCannotCreateVenue.getStatusCode());

        LocalDate date = LocalDate.now().plusDays(1);
        Map<String, Object> bookingRequest = Map.of(
                "venueId", venueId,
                "bookingDate", date.toString(),
                "startTime", "18:00",
                "durationHours", 1
        );

        ResponseEntity<Map> booking = restTemplate.exchange("/api/bookings", HttpMethod.POST,
                new HttpEntity<>(bookingRequest, playerHeaders), Map.class);
        assertEquals(HttpStatus.OK, booking.getStatusCode());
        assertEquals(180000, ((Number) booking.getBody().get("totalPrice")).longValue());

        ResponseEntity<List<Map<String, Object>>> availability = restTemplate.exchange(
                "/api/venues/" + venueId + "/availability?date=" + date,
                HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, availability.getStatusCode());
        assertEquals(1, availability.getBody().size());

        ResponseEntity<List<Map<String, Object>>> playerBookings = restTemplate.exchange("/api/bookings",
                HttpMethod.GET, new HttpEntity<>(playerHeaders), new ParameterizedTypeReference<>() {});
        assertEquals(1, playerBookings.getBody().size());

        ResponseEntity<Map<String, Object>> playerProfile = restTemplate.exchange("/api/auth/me",
                HttpMethod.GET, new HttpEntity<>(playerHeaders), new ParameterizedTypeReference<>() {});
        assertEquals("PLAYER", playerProfile.getBody().get("role"));
        ResponseEntity<Map<String, Object>> updatedProfile = restTemplate.exchange("/api/auth/me",
                HttpMethod.PUT, new HttpEntity<>(Map.of(
                        "fullName", "Updated Player",
                        "phone", "0911111111"
                ), playerHeaders), new ParameterizedTypeReference<>() {});
        assertEquals(HttpStatus.OK, updatedProfile.getStatusCode());
        assertEquals("Updated Player", updatedProfile.getBody().get("fullName"));

        String otherPlayerToken = register("other-player");
        HttpHeaders otherPlayerHeaders = bearer(otherPlayerToken);
        ResponseEntity<List<Map<String, Object>>> otherPlayerBookings = restTemplate.exchange("/api/bookings",
                HttpMethod.GET, new HttpEntity<>(otherPlayerHeaders), new ParameterizedTypeReference<>() {});
        assertEquals(0, otherPlayerBookings.getBody().size());

        Long bookingId = ((Number) booking.getBody().get("id")).longValue();
        ResponseEntity<Map> cannotCancelAnotherPlayersBooking = restTemplate.exchange(
                "/api/bookings/" + bookingId, HttpMethod.DELETE,
                new HttpEntity<>(otherPlayerHeaders), Map.class);
        assertEquals(HttpStatus.NOT_FOUND, cannotCancelAnotherPlayersBooking.getStatusCode());

        ResponseEntity<List<Map<String, Object>>> ownerBookings = restTemplate.exchange("/api/bookings/managed",
                HttpMethod.GET, new HttpEntity<>(ownerHeaders), new ParameterizedTypeReference<>() {});
        assertEquals(1, ownerBookings.getBody().size());

        ResponseEntity<Map> conflict = restTemplate.exchange("/api/bookings", HttpMethod.POST,
                new HttpEntity<>(bookingRequest, ownerHeaders), Map.class);
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());

        ResponseEntity<Map> cancelled = restTemplate.exchange("/api/bookings/" + bookingId,
                HttpMethod.DELETE, new HttpEntity<>(playerHeaders), Map.class);
        assertEquals(HttpStatus.OK, cancelled.getStatusCode());
        assertEquals("CANCELLED", cancelled.getBody().get("status"));
        ResponseEntity<List<Map<String, Object>>> releasedSlot = restTemplate.exchange(
                "/api/venues/" + venueId + "/availability?date=" + date,
                HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {});
        assertEquals(0, releasedSlot.getBody().size());

        ResponseEntity<Void> archivedVenue = restTemplate.exchange("/api/venues/" + venueId,
                HttpMethod.DELETE, new HttpEntity<>(ownerHeaders), Void.class);
        assertEquals(HttpStatus.OK, archivedVenue.getStatusCode());
        ResponseEntity<List<Map<String, Object>>> publicVenues = restTemplate.exchange("/api/venues",
                HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<>() {});
        assertEquals(false, publicVenues.getBody().stream()
                .anyMatch(venue -> ((Number) venue.get("id")).longValue() == venueId));
        ResponseEntity<List<Map<String, Object>>> retainedOwnerBookings = restTemplate.exchange("/api/bookings/managed",
                HttpMethod.GET, new HttpEntity<>(ownerHeaders), new ParameterizedTypeReference<>() {});
        assertEquals(1, retainedOwnerBookings.getBody().size());

        HttpHeaders anonymousHeaders = new HttpHeaders();
        ResponseEntity<String> forbiddenRead = restTemplate.exchange("/api/bookings", HttpMethod.GET,
                new HttpEntity<>(anonymousHeaders), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, forbiddenRead.getStatusCode());
    }

    private String register(String suffix) {
        String email = suffix + "-" + System.nanoTime() + "@example.test";
        Map<String, Object> request = Map.of(
                "fullName", "Test " + suffix,
                "email", email,
                "phone", "0900000000",
                "password", "test-password-123",
                "role", "OWNER"
        );
        ResponseEntity<Map> registration = restTemplate.postForEntity("/api/auth/register", request, Map.class);
        assertEquals(HttpStatus.OK, registration.getStatusCode());
        assertEquals("PLAYER", ((Map<?, ?>) registration.getBody().get("account")).get("role"));
        return login(email, "test-password-123");
    }

    private String login(String email, String password) {
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/login",
                Map.of("email", email, "password", password), Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return (String) response.getBody().get("token");
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
