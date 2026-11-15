package com.codecool.flightapi.auth;

import com.codecool.flightapi.common.error.InvalidRefreshTokenException;
import com.codecool.flightapi.security.UserPrincipal;
import com.codecool.flightapi.user.User;
import com.codecool.flightapi.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates and revokes refresh tokens. A token is a random 256-bit value given to the client once; only
 * its SHA-256 hash is stored. Each refresh uses up the presented token and issues the next one of the same
 * family. A used token presented again means it was copied: the whole family is revoked, ending that login on
 * every device that holds a token of it.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    /** A token value for the client, with its expiry. */
    public record IssuedToken(String value, Instant expiresAt) {}

    /** The account a refresh was for, and the token that replaces the presented one. */
    public record Rotation(UserPrincipal principal, IssuedToken next) {}

    /** Starts a new family for a successful login. */
    @Transactional
    public IssuedToken issue(Long userId) {
        return issue(userRepository.getReferenceById(userId), UUID.randomUUID());
    }

    /**
     * Exchanges a refresh token for the next one, and returns the account with its current roles.
     *
     * <p>A token used again within {@link RefreshTokenProperties#reuseGrace} of its first use (two tabs refreshing
     * at once) gets its own next token in the same family.
     *
     * @throws InvalidRefreshTokenException if the token is unknown, expired, revoked or was used before the grace
     *     period; reuse after the grace period also revokes its family
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Rotation rotate(String tokenValue) {
        Instant now = clock.instant();
        RefreshToken token =
                refreshTokenRepository.findByTokenHash(hash(tokenValue)).orElseThrow(InvalidRefreshTokenException::new);
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException();
        }
        // load the account before markUsed clears the persistence context
        User user = token.getUser();
        UserPrincipal principal = UserPrincipal.build(user);
        if (token.getUsedAt() != null || refreshTokenRepository.markUsed(token.getId(), now) == 0) {
            // markUsed found it used: a concurrent refresh marked it a moment ago
            Instant usedAt = token.getUsedAt() != null ? token.getUsedAt() : now;
            if (withinReuseGrace(usedAt, now)) {
                log.info("Refresh token reused within the grace period (family {})", token.getFamilyId());
                return new Rotation(principal, issue(user, token.getFamilyId()));
            }
            log.warn("Refresh token reused; revoking its family {} (user id {})", token.getFamilyId(), user.getId());
            refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        return new Rotation(principal, issue(user, token.getFamilyId()));
    }

    /** Ends the login the token belongs to. Unknown tokens are ignored, so logout always succeeds. */
    @Transactional
    public void revoke(String tokenValue) {
        refreshTokenRepository
                .findByTokenHash(hash(tokenValue))
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant()));
    }

    /** Expired tokens can never be used again; delete them nightly. */
    @Scheduled(cron = "${app.refresh-token.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpired(clock.instant());
        log.info("Deleted {} expired refresh tokens", deleted);
    }

    private boolean withinReuseGrace(Instant usedAt, Instant now) {
        Duration grace = properties.reuseGrace();
        return !grace.isZero() && !now.isAfter(usedAt.plus(grace));
    }

    private IssuedToken issue(User user, UUID familyId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.lifetime());
        refreshTokenRepository.save(new RefreshToken(user, familyId, hash(value), now, expiresAt));
        return new IssuedToken(value, expiresAt);
    }

    /** Hex SHA-256. Tokens are random 256-bit values, so a fast hash without salt is enough. */
    static String hash(String tokenValue) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java platform", e);
        }
    }
}
