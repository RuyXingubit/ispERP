package br.dev.xb.isperp.entity;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LocalAuditableEntity {

    @Nullable
    UUID getId();

    void setId(UUID id);

    @Nullable
    LocalDateTime getCreatedAt();

    void setCreatedAt(LocalDateTime createdAt);

    @Nullable
    LocalDateTime getUpdatedAt();

    void setUpdatedAt(LocalDateTime updatedAt);

    default void initEntityDefaults() {
    }
}
