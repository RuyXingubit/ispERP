package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.entity.financial.CashTransferStatus;
import br.dev.xb.isperp.entity.financial.CustodyTransferLogEntity;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@SuppressWarnings({"null", "java:S2583"})
public class EntityLifecycleListener {

    @PrePersist
    public void onPrePersist(Object entity) {
        if (entity instanceof LocalAuditableEntity localEntity) {
            localEntity.setId(UuidCreatorUtils.orGenerate(localEntity.getId()));
            LocalDateTime now = LocalDateTime.now();
            if (localEntity.getCreatedAt() == null) {
                localEntity.setCreatedAt(now);
            }
            if (localEntity.getUpdatedAt() == null) {
                localEntity.setUpdatedAt(now);
            }
            localEntity.initEntityDefaults();
        } else if (entity instanceof OffsetAuditableEntity offsetEntity) {
            offsetEntity.setId(UuidCreatorUtils.orGenerate(offsetEntity.getId()));
            OffsetDateTime now = OffsetDateTime.now();
            if (offsetEntity.getCreatedAt() == null) {
                offsetEntity.setCreatedAt(now);
            }
            if (offsetEntity.getUpdatedAt() == null) {
                offsetEntity.setUpdatedAt(now);
            }
            offsetEntity.initEntityDefaults();
        } else if (entity instanceof CustodyTransferLogEntity transferEntity) {
            transferEntity.setId(UuidCreatorUtils.orGenerate(transferEntity.getId()));
            if (transferEntity.getRequestedAt() == null) {
                transferEntity.setRequestedAt(OffsetDateTime.now());
            }
            if (transferEntity.getStatus() == null) {
                transferEntity.setStatus(CashTransferStatus.PENDING_ACCEPTANCE);
            }
            transferEntity.initEntityDefaults();
        }
    }

    @PreUpdate
    public void onPreUpdate(Object entity) {
        if (entity instanceof LocalAuditableEntity localEntity) {
            localEntity.setUpdatedAt(LocalDateTime.now());
        } else if (entity instanceof OffsetAuditableEntity offsetEntity) {
            offsetEntity.setUpdatedAt(OffsetDateTime.now());
        }
    }
}
