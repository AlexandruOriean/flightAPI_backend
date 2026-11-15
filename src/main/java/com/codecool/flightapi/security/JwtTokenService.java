package com.codecool.flightapi.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues access tokens for authenticated users. Validation is handled by Spring Security's
 * OAuth2 resource server using the {@code JwtDecoder} from {@link JwtConfig}.
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    private final JwtProperties properties;

    /** How long an issued token stays valid. */
    public Duration getTokenLifetime() {
        return properties.tokenLifetime();
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(getTokenLifetime()))
                .claim(JwtConfig.ROLES_CLAIM, roles)
                .build();

        JwsHeader header = JwsHeader.with(JwtConfig.ALGORITHM).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
