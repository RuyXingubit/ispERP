package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "onu_provisionings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class OnuProvisioning {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do contrato é obrigatório")
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @NotNull(message = "ID do cliente é obrigatório")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "network_device_id")
    private UUID networkDeviceId;

    @NotBlank(message = "MAC da ONU é obrigatório")
    @Column(name = "onu_mac", nullable = false, length = 50)
    private String onuMac;

    @NotBlank(message = "Número de série da ONU é obrigatório")
    @Column(name = "onu_serial", nullable = false, length = 50)
    private String onuSerial;

    @Column(name = "vlan_id", nullable = false)
    @Builder.Default
    private Integer vlanId = 100;

    @Column(name = "pppoe_user", length = 100)
    private String pppoeUser;

    @Column(name = "pppoe_password", length = 100)
    private String pppoePassword;

    @NotNull(message = "Velocidade de download é obrigatória")
    @Column(name = "download_speed", nullable = false)
    private Integer downloadSpeed; // Mbps

    @NotNull(message = "Velocidade de upload é obrigatória")
    @Column(name = "upload_speed", nullable = false)
    private Integer uploadSpeed; // Mbps

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OnuStatus status = OnuStatus.PROVISIONED;

    @Column(name = "rx_power_dbm", precision = 5, scale = 2)
    private BigDecimal rxPowerDbm;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = OnuStatus.PROVISIONED;
        }
        this.lastSyncAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum OnuStatus {
        PROVISIONED,
        BLOCKED,
        DEPROVISIONED
    }
}
