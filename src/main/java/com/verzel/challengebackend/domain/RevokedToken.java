package com.verzel.challengebackend.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Implements {@link Persistable} for the same reason as {@link User}: {@code jti} is always
 * pre-assigned from the JWT claim before saving, so without this Spring Data R2DBC would issue
 * a no-op UPDATE instead of an INSERT on every logout.
 */
@Table("revoked_tokens")
public class RevokedToken implements Persistable<UUID> {

    @Id
    private final UUID jti;
    private final UUID userId;
    private final OffsetDateTime expiresAt;
    private final OffsetDateTime revokedAt;

    @Transient
    private final boolean isNew;

    public RevokedToken(UUID jti, UUID userId, OffsetDateTime expiresAt, OffsetDateTime revokedAt) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return jti;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getJti() {
        return jti;
    }

    public UUID getUserId() {
        return userId;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }
}
