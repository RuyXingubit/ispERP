package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "network_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkDevice {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Nome do dispositivo de rede é obrigatório")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "device_type", nullable = false, length = 50)
    @Builder.Default
    private String deviceType = "OLT"; // OLT, BRAS_PPPOE, RADIUS_SERVER

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Tipo de driver é obrigatório")
    @Column(name = "driver_type", nullable = false, length = 50)
    @Builder.Default
    private NetworkDriverType driverType = NetworkDriverType.SMARTOLT;

    @NotBlank(message = "Endereço IP / Hostname é obrigatório")
    @Column(name = "ip_address", nullable = false, length = 100)
    private String ipAddress;

    @Column(name = "api_port")
    @Builder.Default
    private Integer apiPort = 443;

    @Column(name = "api_token")
    private String apiToken;

    @Column(name = "snmp_community", length = 100)
    private String snmpCommunity;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
