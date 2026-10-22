package br.dev.xb.isperp.entity;

import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface OffsetAuditableEntity {

    @Nullable
    UUID getId();

    void setId(UUID id);

    @Nullable
    OffsetDateTime getCreatedAt();

    void setCreatedAt(OffsetDateTime createdAt);

    @Nullable
    OffsetDateTime getUpdatedAt();

    void setUpdatedAt(OffsetDateTime updatedAt);

    default void initEntityDefaults() {
    }
}
