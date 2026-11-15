package com.codecool.flightapi.auth;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codecool.flightapi.common.error.InvalidRefreshTokenException;
import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer of {@link AuthController}. The slice runs with Spring Boot's default security, so requests carry
 * a mock user and a CSRF token; the real rules (these endpoints are public) are tested in {@code AccessRulesIT}
 * and the full flow in {@code AuthenticationFlowIT}.
 */
@WebMvcTest(AuthController.class)
@WithMockUser
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private AuthService authService;

    private final TokenResponse tokens = new TokenResponse(
            "access", "Bearer", 900, "refresh", 604800, 1L, "alice", "alice@example.com", List.of("ROLE_USER"));

    @Test
    @DisplayName("POST /register -> 201 with a message")
    void register() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","name":"Alice","email":"alice@example.com","password":"secret-password"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));

        verify(registrationService)
                .register(new RegisterRequest("alice", "Alice", "alice@example.com", "secret-password"));
    }

    @Test
    @DisplayName("POST /register with invalid fields, including a password over 72 bytes -> 400")
    void invalidRegistration() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"a b","email":"not-an-email","password":"%s"}
                                """.formatted("ä".repeat(40))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder("username", "email", "passwordWithinByteLimit")));
        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("POST /register with a taken username or e-mail -> 409")
    void duplicateRegistration() throws Exception {
        doThrow(new ResourceAlreadyExistsException("Username or e-mail is already in use."))
                .when(registrationService)
                .register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"secret-password"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Username or e-mail is already in use."));
    }

    @Test
    @DisplayName("POST /login -> 200 with both tokens, no cookie")
    void login() throws Exception {
        when(authService.login(new LoginRequest("alice", "secret-password"))).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret-password\"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
    }

    @Test
    @DisplayName("POST /login with bad credentials -> 401 problem+json with WWW-Authenticate, no hint which part")
    void loginWithBadCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("user not found"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    @DisplayName("POST /login without password -> 400")
    void loginWithoutPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /refresh -> 200 with a new pair")
    void refresh() throws Exception {
        when(authService.refresh(new RefreshRequest("refresh"))).thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    @DisplayName("POST /refresh with an invalid token -> 401 asking to log in again")
    void refreshWithInvalidToken() throws Exception {
        when(authService.refresh(any(RefreshRequest.class))).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"used\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("The refresh token is invalid or expired. Log in again."));
    }

    @Test
    @DisplayName("POST /refresh without a token -> 400")
    void refreshWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /password -> 200 with a new login for the signed-in user")
    void changePassword() throws Exception {
        when(authService.changePassword("user", new ChangePasswordRequest("old-secret", "new-secret")))
                .thenReturn(tokens);

        mockMvc.perform(post("/api/v1/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-secret\",\"newPassword\":\"new-secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    @DisplayName("POST /password with a too short or unchanged new password -> 400")
    void invalidPasswordChange() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-secret\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));
        mockMvc.perform(post("/api/v1/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"same-secret\",\"newPassword\":\"same-secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("newPasswordDifferent"));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /logout -> 204, revokes the refresh token and expires the legacy cookie")
    void logout() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                                "Set-Cookie",
                                allOf(
                                        containsString("token="),
                                        containsString("Path=/users"),
                                        containsString("Max-Age=0"),
                                        containsString("HttpOnly"),
                                        containsString("SameSite=Strict"))));

        verify(authService).logout(new LogoutRequest("refresh"));
    }

    @Test
    @DisplayName("POST /logout without a body -> 204; the cookie is Secure over HTTPS")
    void logoutWithoutBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").with(csrf()).secure(true))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("Secure")));

        verify(authService).logout(isNull());
    }
}
