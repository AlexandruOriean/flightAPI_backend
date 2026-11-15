package com.codecool.flightapi.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Tokens issued by {@link JwtTokenService} at a fixed time, read back with the application's decoder. */
class JwtTokenServiceTest {

    // Test-only key (64 random bytes, Base64). Never use outside tests.
    private static final String SECRET =
            "dGVzdC1vbmx5LWtleS1kby1ub3QtdXNlLWluLXByb2R1Y3Rpb24tMDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3A=";

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    private final JwtConfig config = new JwtConfig();
    private final SecretKey key = config.jwtSigningKey(new JwtProperties(SECRET, 900_000));
    private final JwtTokenService service = new JwtTokenService(
            config.jwtEncoder(key), new JwtProperties(SECRET, 900_000), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("the token names the user, carries the roles and expires one lifetime after it was issued")
    void issuesTokenWithClaims() {
        String token = service.generateToken(UsernamePasswordAuthenticationToken.authenticated(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        Jwt jwt = decodeWithoutExpiryCheck(token);
        assertThat(jwt.getSubject()).isEqualTo("alice");
        assertThat(jwt.getClaimAsStringList(JwtConfig.ROLES_CLAIM)).containsExactly("ROLE_USER");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(service.getTokenLifetime().toSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("the application's decoder accepts the token's signature and algorithm")
    void tokenIsSignedWithTheConfiguredKey() {
        String token =
                service.generateToken(UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of()));
        String header = new String(Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))), UTF_8);

        assertThat(header).contains("\"alg\":\"HS512\"");
        // a fixed clock in the past: the real decoder would reject it as expired, so only the signature is checked
        assertThat(decodeWithoutExpiryCheck(token).getTokenValue()).isEqualTo(token);
    }

    private Jwt decodeWithoutExpiryCheck(String token) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(JwtConfig.ALGORITHM)
                .build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder.decode(token);
    }
}
