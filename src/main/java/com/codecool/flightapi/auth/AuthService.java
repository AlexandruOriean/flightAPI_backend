package com.codecool.flightapi.auth;

import com.codecool.flightapi.common.error.InvalidRefreshTokenException;
import com.codecool.flightapi.security.JwtTokenService;
import com.codecool.flightapi.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Login, token refresh and logout. Each successful login or refresh yields a short-lived access token (JWT) and
 * a single-use refresh token; see {@link RefreshTokenService} for how refresh tokens are rotated and revoked.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenProperties refreshTokenProperties;

    /**
     * Checks the credentials and starts a new login.
     *
     * @throws AuthenticationException on bad credentials (rendered as 401)
     */
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return tokens(principal, refreshTokenService.issue(principal.getId()));
    }

    /**
     * Exchanges a refresh token for a new pair; the presented token is used up.
     *
     * @throws InvalidRefreshTokenException if it is unknown, expired, revoked or already used (rendered as 401)
     */
    public TokenResponse refresh(RefreshRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());
        return tokens(rotation.principal(), rotation.next());
    }

    /** Ends the login the refresh token belongs to, if one is given. Always succeeds. */
    public void logout(LogoutRequest request) {
        if (request != null && request.refreshToken() != null) {
            refreshTokenService.revoke(request.refreshToken());
        }
    }

    private TokenResponse tokens(UserPrincipal principal, RefreshTokenService.IssuedToken refreshToken) {
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new TokenResponse(
                jwtTokenService.generateToken(authentication),
                "Bearer",
                jwtTokenService.getTokenLifetime().toSeconds(),
                refreshToken.value(),
                // every refresh token, also a rotated one, gets the full configured lifetime
                refreshTokenProperties.lifetime().toSeconds(),
                principal.getId(),
                principal.getUsername(),
                principal.getEmail(),
                roles);
    }
}
