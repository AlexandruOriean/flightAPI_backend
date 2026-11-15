package com.codecool.flightapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codecool.flightapi.common.error.InvalidRefreshTokenException;
import com.codecool.flightapi.security.JwtTokenService;
import com.codecool.flightapi.security.UserPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final RefreshTokenService.IssuedToken REFRESH =
            new RefreshTokenService.IssuedToken("refresh-1", Instant.parse("2026-11-22T12:00:00Z"));

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    private final UserPrincipal alice = new UserPrincipal(
            7L, "alice", "alice@example.com", "{bcrypt}hash", List.of(new SimpleGrantedAuthority("ROLE_USER")));

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                jwtTokenService,
                refreshTokenService,
                new RefreshTokenProperties(Duration.ofDays(7)));
    }

    @Test
    void loginReturnsBothTokensWithTheAccountAndItsRoles() {
        when(authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("alice", "secret")))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(alice, null, alice.getAuthorities()));
        when(refreshTokenService.issue(7L)).thenReturn(REFRESH);
        stubAccessToken();

        TokenResponse tokens = authService.login(new LoginRequest("alice", "secret"));

        assertThat(tokens)
                .isEqualTo(new TokenResponse(
                        "access-1",
                        "Bearer",
                        900,
                        "refresh-1",
                        7 * 24 * 3600,
                        7L,
                        "alice",
                        "alice@example.com",
                        List.of("ROLE_USER")));
    }

    @Test
    void failedLoginIssuesNoTokens() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verifyNoInteractions(refreshTokenService, jwtTokenService);
    }

    @Test
    void refreshReturnsTheNextPairForTheRotatedToken() {
        when(refreshTokenService.rotate("refresh-0")).thenReturn(new RefreshTokenService.Rotation(alice, REFRESH));
        stubAccessToken();

        TokenResponse tokens = authService.refresh(new RefreshRequest("refresh-0"));

        assertThat(tokens.accessToken()).isEqualTo("access-1");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-1");
        assertThat(tokens.username()).isEqualTo("alice");
    }

    @Test
    void refreshWithAnInvalidTokenIssuesNoAccessToken() {
        when(refreshTokenService.rotate("stolen")).thenThrow(new InvalidRefreshTokenException());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("stolen")))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void logoutRevokesTheGivenRefreshTokenAndIgnoresAMissingOne() {
        authService.logout(new LogoutRequest("refresh-1"));
        verify(refreshTokenService).revoke("refresh-1");

        authService.logout(null);
        authService.logout(new LogoutRequest(null));
        verify(refreshTokenService, never()).revoke(null);
    }

    private void stubAccessToken() {
        when(jwtTokenService.generateToken(any(Authentication.class))).thenReturn("access-1");
        when(jwtTokenService.getTokenLifetime()).thenReturn(Duration.ofMinutes(15));
    }
}
