package com.codecool.flightapi.security;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The authorization rules of {@link SecurityConfig} as a table: who may call what. Runs the full
 * application (default profile, i.e. without the dev-only tooling chain).
 */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:access-rules-it;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            // Test-only key (64 random bytes, Base64). Never use outside tests.
            "app.jwt.secret=dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24tMDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3A=",
            "app.admin.username=rules-admin",
            "app.admin.email=rules-admin@example.com",
            "app.admin.password=" + AccessRulesIT.ADMIN_PASSWORD,
            // every request comes from 127.0.0.1 and these tests log in and register many times;
            // the limits themselves are tested in RateLimitIT
            "app.rate-limit.login.requests=1000",
            "app.rate-limit.register.requests=1000",
            "app.cors.allowed-origins=" + AccessRulesIT.FRONTEND_ORIGIN
        })
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccessRulesIT {

    static final String FRONTEND_ORIGIN = "https://frontend.example.com";
    private static final String OTHER_ORIGIN = "https://evil.example.com";
    static final String ADMIN_PASSWORD = "rules-admin-Secret-123!";
    private static final String USER_PASSWORD = "Secret123!";

    @Autowired
    private MockMvc mockMvc;

    private String userToken;
    private String adminToken;

    @BeforeAll
    void obtainTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"rules-user\",\"name\":\"R\",\"email\":\"rules-user@example.com\","
                                + "\"password\":\"" + USER_PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        userToken = login("rules-user", USER_PASSWORD);
        adminToken = login("rules-admin", ADMIN_PASSWORD);
    }

    @ParameterizedTest(name = "{0} {1} {2} -> {3}")
    @CsvSource(delimiter = '|', textBlock = """
            # caller    | method | path                                   | expected status
            # --- public: what the frontend uses before login, probes ---
            anonymous   | GET    | /api/v1/flights/1                      | 200
            anonymous   | GET    | /api/v1/flights?from=London&to=Henri&date=2020-11-01 | 200
            # a partial search is a 400, not a way into the admin-only list
            anonymous   | GET    | /api/v1/flights?from=London            | 400
            anonymous   | GET    | /api/v1/flights?date=2020-11-01        | 400
            # refresh is public: without a body it is a 400 (validation), not a 401
            anonymous   | POST   | /api/v1/auth/refresh                   | 400
            anonymous   | GET    | /actuator/health                       | 200
            anonymous   | GET    | /actuator/info                         | 200
            # --- everything else needs a token (previously public by accident) ---
            anonymous   | GET    | /api/v1/flights                        | 401
            anonymous   | GET    | /api/v1/flights?page=0&size=5          | 401
            anonymous   | POST   | /api/v1/flights?from=London            | 401
            anonymous   | GET    | /api/v1/flights/London/Henri/2020-11-01 | 401
            anonymous   | POST   | /users/login                           | 401
            anonymous   | GET    | /api/v1/aircraft-types/1                    | 401
            anonymous   | GET    | /api/v1/airports/names                 | 401
            anonymous   | POST   | /api/v1/cities                         | 401
            anonymous   | GET    | /actuator/env                          | 401
            anonymous   | GET    | /h2                                    | 401
            anonymous   | GET    | /swagger-ui.html                       | 401
            anonymous   | GET    | /not/a/known/path                      | 401
            # --- signed-in user: reads yes, writes and admin reads no ---
            user        | GET    | /api/v1/aircraft-types/1                    | 200
            user        | GET    | /api/v1/airports/names                 | 200
            user        | GET    | /api/v1/flights                        | 403
            user        | GET    | /api/v1/flights?page=0                 | 403
            user        | POST   | /api/v1/airlines                       | 403
            user        | PUT    | /api/v1/cities/1                       | 403
            user        | DELETE | /api/v1/cities/1                       | 403
            user        | GET    | /actuator/env                          | 403
            user        | GET    | /not/a/known/path                      | 403
            # --- admin ---
            admin       | GET    | /api/v1/flights                        | 200
            admin       | PATCH  | /api/v1/cities/1                       | 403
            """)
    void accessRules(String caller, String method, String path, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path);
        switch (caller) {
            case "user" -> request.header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken);
            case "admin" -> request.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken);
            case "anonymous" -> {}
            default -> throw new IllegalArgumentException(caller);
        }
        mockMvc.perform(request).andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("API responses must not be framed (clickjacking protection is no longer disabled)")
    void apiResponsesDenyFraming() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/api/v1/flights/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("CORS preflight from the configured frontend origin is allowed")
    void corsPreflightFromAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/airlines")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    @DisplayName("CORS lets the frontend send If-Match on PUT and read ETag and Retry-After")
    void corsAllowsConditionalUpdates() throws Exception {
        mockMvc.perform(options("/api/v1/airlines/1")
                        .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type,if-match"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                                HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("if-match")));
        mockMvc.perform(get("/api/v1/flights/1").header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, matchesPattern("\"\\d+\"")))
                .andExpect(header().string(
                                HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                                allOf(containsString("ETag"), containsString("Retry-After"))));
    }

    @Test
    @DisplayName("CORS preflight from any other origin is rejected (previously every origin was allowed)")
    void corsPreflightFromOtherOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/flights/1")
                        .header(HttpHeaders.ORIGIN, OTHER_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Cross-origin requests from other origins are rejected, even to public endpoints")
    void corsRequestFromOtherOriginIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/flights/1").header(HttpHeaders.ORIGIN, OTHER_ORIGIN))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/flights/1").header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN));
    }

    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }
}
