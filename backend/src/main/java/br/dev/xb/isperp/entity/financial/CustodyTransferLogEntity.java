package br.dev.xb.isperp.entity.financial;

import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CustodyTransferLogEntity {

    @Nullable
    UUID getId();

    void setId(UUID id);

    @Nullable
    CashTransferStatus getStatus();

    void setStatus(CashTransferStatus status);

    @Nullable
    OffsetDateTime getRequestedAt();

    void setRequestedAt(OffsetDateTime requestedAt);

    default void initEntityDefaults() {
    }
}
