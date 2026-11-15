package com.codecool.flightapi.auth;

import com.codecool.flightapi.common.web.MessageResponse;
import com.codecool.flightapi.security.JwtTokenService;
import com.codecool.flightapi.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AuthController {

    /** Set by login in earlier versions; the token is now only returned in the response body. */
    private static final String LEGACY_TOKEN_COOKIE = "token";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RegistrationService registrationService;

    /** 201 on success; 409 if the username or e-mail is taken. */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("Registration successful"));
    }

    /**
     * 200 with the access token; 401 (problem+json) on bad credentials. The client sends the token back
     * as {@code Authorization: Bearer <token>}; no cookie is set.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        String jwt = jwtTokenService.generateToken(authentication);

        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(
                new TokenResponse(jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles));
    }

    /**
     * 204. Tokens are stateless, so logging out means the client discards its token; it stays valid
     * until it expires. Also expires the token cookie that earlier versions set at login.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutUser(HttpServletRequest request) {
        ResponseCookie expired = ResponseCookie.from(LEGACY_TOKEN_COOKIE, "")
                // the legacy cookie had no explicit path, so browsers scoped it to /users
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
