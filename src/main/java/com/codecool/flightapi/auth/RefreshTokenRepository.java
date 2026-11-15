package com.codecool.flightapi.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** The user and their roles are needed for the next access token; load them in the same query. */
    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Marks the token used unless it already is. Atomic, so of two concurrent refreshes with the same token
     * exactly one succeeds.
     *
     * @return 1 if this call marked it, 0 if it was already used
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.usedAt = :now where t.id = :id and t.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    /** Ends every login of the user, e.g. after a password change. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllOfUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
