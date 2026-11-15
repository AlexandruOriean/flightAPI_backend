package com.codecool.flightapi.auth;

import com.codecool.flightapi.common.web.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Set by login in earlier versions; the token is now only returned in the response body. */
    private static final String LEGACY_TOKEN_COOKIE = "token";

    private final RegistrationService registrationService;
    private final AuthService authService;

    /** 201 on success; 409 if the username or e-mail is taken. */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Registration successful"));
    }

    /**
     * 200 with an access token and a refresh token; 401 (problem+json) on bad credentials. The client sends the
     * access token back as {@code Authorization: Bearer <token>}; no cookie is set.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * 200 with a new access token and a new refresh token; the presented refresh token is used up. 401
     * (problem+json) if it is unknown, expired, revoked or already used.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Changes the signed-in user's password: 200 with a new login for this client; every other login of the
     * account ends. 400 if the current password is wrong or the new one breaks the rules.
     */
    @PostMapping("/password")
    public ResponseEntity<TokenResponse> changePassword(
            Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(authService.changePassword(authentication.getName(), request));
    }

    /**
     * 204. With a refresh token in the body, that login's refresh tokens are revoked; the access token is
     * stateless and stays valid until it expires (minutes). Also expires the token cookie that earlier versions
     * set at login.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(
            HttpServletRequest request, @Valid @RequestBody(required = false) LogoutRequest body) {
        authService.logout(body);
        ResponseCookie expired = ResponseCookie.from(LEGACY_TOKEN_COOKIE, "")
                // the legacy cookie was set by /users/login without an explicit path, so browsers scoped it to /users
                .path("/users")
                .maxAge(0)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Strict")
                .build();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .build();
    }
}
