package com.codecool.flightapi.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The rate limits of the authentication endpoints in the full application: 2 logins/min, 1 registration/h. */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:rate-limit-it;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            // Test-only key (64 random bytes, Base64). Never use outside tests.
            "app.jwt.secret=dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24tMDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3A=",
            "app.rate-limit.login.requests=2",
            "app.rate-limit.login.period=1m",
            "app.rate-limit.register.requests=1",
            "app.rate-limit.register.period=1h"
        })
@AutoConfigureMockMvc
class RateLimitIT {

    private static final String BAD_LOGIN = "{\"username\":\"nobody\",\"password\":\"wrong-password\"}";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("login: attempts over the limit -> 429 problem+json with Retry-After, before authentication")
    void loginOverLimitIsTooManyRequests() throws Exception {
        mockMvc.perform(login("203.0.113.1")).andExpect(status().isUnauthorized());
        mockMvc.perform(login("203.0.113.1")).andExpect(status().isUnauthorized());

        mockMvc.perform(login("203.0.113.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"))
                .andExpect(jsonPath("$.title").value("Too many requests"))
                .andExpect(jsonPath("$.detail").value(containsString("Try again in 30 seconds")));
    }

    @Test
    @DisplayName("login: another client IP is not affected")
    void otherClientsAreNotAffected() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login("203.0.113.2"));
        }

        mockMvc.perform(login("203.0.113.3")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("register: the second registration from one IP within the hour -> 429")
    void registrationOverLimitIsTooManyRequests() throws Exception {
        mockMvc.perform(register("203.0.113.4", "first")).andExpect(status().isCreated());

        mockMvc.perform(register("203.0.113.4", "second"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "3600"));
    }

    private static MockHttpServletRequestBuilder login(String clientIp) {
        return post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(BAD_LOGIN);
    }

    private static MockHttpServletRequestBuilder register(String clientIp, String username) {
        return post("/api/v1/auth/register")
                .with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"email\":\"%s@example.com\",\"password\":\"long-enough-1\"}"
                        .formatted(username, username));
    }
}
