package com.codecool.flightapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codecool.flightapi.common.error.InvalidRefreshTokenException;
import com.codecool.flightapi.user.RoleName;
import com.codecool.flightapi.user.RoleRepository;
import com.codecool.flightapi.user.User;
import com.codecool.flightapi.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/** Refresh-token rotation against the real Flyway schema (embedded H2), with a clock the test moves. */
@DataJpaTest
class RefreshTokenServiceTest {

    private static final Duration LIFETIME = Duration.ofDays(7);

    private static final Duration REUSE_GRACE = Duration.ofSeconds(10);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-15T12:00:00Z"));

    private RefreshTokenService service;
    private User alice;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(
                refreshTokenRepository, userRepository, new RefreshTokenProperties(LIFETIME, REUSE_GRACE), clock);
        alice = userRepository.save(new User(
                null,
                "Alice",
                "alice",
                "{bcrypt}hash",
                "alice@example.com",
                Set.of(roleRepository.getByName(RoleName.ROLE_USER))));
    }

    @Test
    @DisplayName("only the SHA-256 hash of a token is stored, never the token itself")
    void storesOnlyTheHash() {
        RefreshTokenService.IssuedToken issued = service.issue(alice.getId());

        assertThat(issued.value()).hasSize(43); // 256 bits, Base64url without padding
        assertThat(issued.expiresAt()).isEqualTo(clock.instant().plus(LIFETIME));
        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> assertThat(token.getTokenHash())
                        .isEqualTo(RefreshTokenService.hash(issued.value()))
                        .isNotEqualTo(issued.value()));
    }

    @Test
    @DisplayName("a refresh returns the account with its roles and a new token; the old one is used up")
    void rotationIssuesTheNextToken() {
        RefreshTokenService.IssuedToken first = service.issue(alice.getId());

        RefreshTokenService.Rotation rotation = service.rotate(first.value());

        assertThat(rotation.principal().getUsername()).isEqualTo("alice");
        assertThat(rotation.principal().getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
        assertThat(rotation.next().value()).isNotEqualTo(first.value());
        assertThat(service.rotate(rotation.next().value()).principal().getUsername())
                .isEqualTo("alice");
    }

    @Test
    @DisplayName("marking a token used succeeds once, so two concurrent refreshes cannot both win")
    void markUsedSucceedsOnce() {
        Long id = refreshTokenRepository
                .findByTokenHash(
                        RefreshTokenService.hash(service.issue(alice.getId()).value()))
                .orElseThrow()
                .getId();

        assertThat(refreshTokenRepository.markUsed(id, clock.instant())).isEqualTo(1);
        assertThat(refreshTokenRepository.markUsed(id, clock.instant())).isZero();
    }

    @Test
    @DisplayName("reusing a used token after the grace period fails and revokes every token of that login")
    void reuseRevokesTheFamily() {
        RefreshTokenService.IssuedToken first = service.issue(alice.getId());
        RefreshTokenService.IssuedToken second = service.rotate(first.value()).next();

        clock.advance(REUSE_GRACE.plusSeconds(1));

        assertThatThrownBy(() -> service.rotate(first.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate(second.value())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("reuse within the grace period (two tabs refreshing at once) gets its own token, nothing revoked")
    void reuseWithinTheGracePeriodGetsItsOwnToken() {
        RefreshTokenService.IssuedToken first = service.issue(alice.getId());
        RefreshTokenService.IssuedToken tabOne = service.rotate(first.value()).next();

        clock.advance(REUSE_GRACE);
        RefreshTokenService.IssuedToken tabTwo = service.rotate(first.value()).next();

        assertThat(tabTwo.value()).isNotEqualTo(tabOne.value());
        assertThat(service.rotate(tabOne.value()).principal().getUsername()).isEqualTo("alice");
        assertThat(service.rotate(tabTwo.value()).principal().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("a revoked login stays revoked, even within the grace period")
    void graceDoesNotReviveARevokedLogin() {
        RefreshTokenService.IssuedToken first = service.issue(alice.getId());
        service.rotate(first.value());

        service.revoke(first.value());

        assertThatThrownBy(() -> service.rotate(first.value())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("with a grace period of 0s every reuse revokes the login, even a concurrent one")
    void zeroGraceIsStrict() {
        service = new RefreshTokenService(
                refreshTokenRepository, userRepository, new RefreshTokenProperties(LIFETIME, Duration.ZERO), clock);
        RefreshTokenService.IssuedToken first = service.issue(alice.getId());
        RefreshTokenService.IssuedToken second = service.rotate(first.value()).next();

        assertThatThrownBy(() -> service.rotate(first.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate(second.value())).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("revokeAll ends every login of the user, but no other user's")
    void revokeAllEndsEveryLoginOfTheUser() {
        User bob = userRepository.save(new User(
                null,
                "Bob",
                "bob",
                "{bcrypt}hash",
                "bob@example.com",
                Set.of(roleRepository.getByName(RoleName.ROLE_USER))));
        RefreshTokenService.IssuedToken laptop = service.issue(alice.getId());
        RefreshTokenService.IssuedToken phone = service.issue(alice.getId());
        RefreshTokenService.IssuedToken bobs = service.issue(bob.getId());

        service.revokeAll(alice.getId());

        assertThatThrownBy(() -> service.rotate(laptop.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate(phone.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(service.rotate(bobs.value()).principal().getUsername()).isEqualTo("bob");
    }

    @Test
    @DisplayName("other logins of the same user are not affected by a revoked family")
    void familiesAreIndependent() {
        RefreshTokenService.IssuedToken laptop = service.issue(alice.getId());
        RefreshTokenService.IssuedToken phone = service.issue(alice.getId());

        service.revoke(laptop.value());

        assertThatThrownBy(() -> service.rotate(laptop.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(service.rotate(phone.value()).principal().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("an expired or unknown token is rejected")
    void expiredOrUnknownTokenIsRejected() {
        RefreshTokenService.IssuedToken issued = service.issue(alice.getId());

        clock.advance(LIFETIME);

        assertThatThrownBy(() -> service.rotate(issued.value())).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.rotate("not-a-token")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @DisplayName("revoking an unknown token does nothing, so logout always succeeds")
    void revokingAnUnknownTokenIsIgnored() {
        service.revoke("not-a-token");

        assertThat(refreshTokenRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("the cleanup deletes expired tokens only")
    void deletesExpiredTokens() {
        service.issue(alice.getId());
        clock.advance(Duration.ofDays(1));
        RefreshTokenService.IssuedToken newer = service.issue(alice.getId());
        clock.advance(LIFETIME.minus(Duration.ofHours(12)));

        service.deleteExpired();

        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(
                        token -> assertThat(token.getTokenHash()).isEqualTo(RefreshTokenService.hash(newer.value())));
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }
    }
}
