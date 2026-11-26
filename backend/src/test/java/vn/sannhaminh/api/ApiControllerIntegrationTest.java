package vn.sannhaminh.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import vn.sannhaminh.config.SecurityConfig;
import vn.sannhaminh.security.JwtService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import vn.sannhaminh.config.ApiExceptionHandler;
import java.util.List;
import java.util.Map;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiController.class)
@Import({ApiExceptionHandler.class, SecurityConfig.class})
class ApiControllerIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean JdbcTemplate db;
    @MockBean JwtService jwtService;

    @Test void healthEndpointReturnsUp() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test @WithMockUser(roles = "PLAYER") void categoriesEndpointReturnsDatabaseRows() throws Exception {
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
}
