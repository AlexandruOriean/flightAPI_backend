package com.codecool.flightapi.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.codecool.flightapi.user.User;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RefreshTokenTest {

    private final User user = new User(1L, "Ana", "ana", "{bcrypt}hash", "ana@example.com", Set.of());

    @Test
    @DisplayName("refresh tokens compare by database id only, like every entity")
    void comparesById() {
        RefreshToken unsaved = token("a");
        RefreshToken sameInstance = unsaved; // AssertJ short-circuits on ==

        assertThat(unsaved.equals(sameInstance)).isTrue();
        assertThat(unsaved).isNotEqualTo(token("a")).isNotEqualTo(null);
        assertThat(token(1L, "a")).isEqualTo(token(1L, "b")).hasSameHashCodeAs(unsaved);
        assertThat(token(1L, "a"))
                .isNotEqualTo(token(2L, "a"))
                .isNotEqualTo(unsaved)
                .isNotEqualTo(new Object());
    }

    private RefreshToken token(Long id, String hash) {
        RefreshToken token = token(hash);
        ReflectionTestUtils.setField(token, "id", id);
        return token;
    }

    private RefreshToken token(String hash) {
        Instant now = Instant.parse("2026-11-15T10:00:00Z");
        return new RefreshToken(user, UUID.randomUUID(), hash, now, now.plusSeconds(60));
    }
}
