package com.codecool.flightapi.auth;

import com.codecool.flightapi.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One refresh token, stored as the SHA-256 hash of its value (V13). Used once: refreshing marks it used and
 * issues the next token of the same {@link #familyId family}.
 */
@Entity
@Table(name = "refresh_token")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Shared by every token issued from one login. */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    /** Hex SHA-256 of the token value; unique (V13). */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** When it was exchanged for the next token; set at most once. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** When its family was revoked (logout or reuse detected). */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    RefreshToken(User user, UUID familyId, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /** Identity is the database id; a new, unsaved token only equals itself. Safe with Hibernate proxies. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof RefreshToken other && id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return RefreshToken.class.hashCode();
    }
}
