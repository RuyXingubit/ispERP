package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "olt_pon_ports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OltPonPort {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_id")
    private @Nullable UUID companyId;

    @NotNull(message = "ID da OLT é obrigatório")
    @Column(name = "network_device_id", nullable = false)
    private UUID networkDeviceId;

    @Column(name = "slot_number", nullable = false)
    @Builder.Default
    private int slotNumber = 0;

    @Column(name = "port_number", nullable = false)
    @Builder.Default
    private int portNumber = 1;

    @NotBlank(message = "Nome da porta PON é obrigatório")
    @Column(name = "pon_name", nullable = false, length = 100)
    private String ponName;

    @Column(name = "admin_status", nullable = false, length = 20)
    @Builder.Default
    private String adminStatus = "UP";

    @Column(name = "oper_status", nullable = false, length = 20)
    @Builder.Default
    private String operStatus = "ACTIVE";

    @Column(name = "tx_power_dbm", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal txPowerDbm = new BigDecimal("4.00");

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal temperatureCelsius = new BigDecimal("42.50");

    @Column(name = "total_onus", nullable = false)
    @Builder.Default
    private int totalOnus = 0;

    @Column(name = "online_onus", nullable = false)
    @Builder.Default
    private int onlineOnus = 0;

    @Column(name = "los_onus", nullable = false)
    @Builder.Default
    private int losOnus = 0;

    @Column(name = "dying_gasp_onus", nullable = false)
    @Builder.Default
    private int dyingGaspOnus = 0;

    @Column(name = "offline_onus", nullable = false)
    @Builder.Default
    private int offlineOnus = 0;

    @Column(name = "connected_cable_id")
    private @Nullable UUID connectedCableId;

    @Column(name = "last_polled_at")
    private @Nullable OffsetDateTime lastPolledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        this.lastPolledAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
