package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.monitoring.OnuSignalStatus;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "onu_telemetry_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnuTelemetryRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @NotNull(message = "ID do provisionamento da ONU é obrigatório")
    @Column(name = "onu_provisioning_id", nullable = false)
    private UUID onuProvisioningId;

    @Column(name = "rx_power_dbm", precision = 5, scale = 2)
    private @Nullable BigDecimal rxPowerDbm;

    @Column(name = "tx_power_dbm", precision = 5, scale = 2)
    private @Nullable BigDecimal txPowerDbm;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_status", nullable = false, length = 30)
    @Builder.Default
    private OnuSignalStatus signalStatus = OnuSignalStatus.ONLINE_GOOD;

    @Column(name = "distance_meters")
    @Builder.Default
    private int distanceMeters = 0;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.recordedAt == null) {
            this.recordedAt = OffsetDateTime.now();
        }
    }
}
