package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tr069_cpe_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tr069CpeConfig {

    @Id
    private UUID id;

    @Column(name = "contract_id", nullable = false, unique = true)
    private UUID contractId;

    @Column(name = "onu_id")
    private UUID onuId;

    @Column(name = "onu_serial", length = 64)
    private String onuSerial;

    @Column(name = "cpe_model", nullable = false, length = 120)
    @Builder.Default
    private String cpeModel = "ONT Dual-Band Wi-Fi 6 Gigabit";

    @Column(name = "firmware_version", nullable = false, length = 64)
    @Builder.Default
    private String firmwareVersion = "V5R020C00S280";

    @Column(name = "uptime_seconds", nullable = false)
    @Builder.Default
    private Long uptimeSeconds = 86400L;

    @Column(name = "ssid_24ghz", nullable = false, length = 32)
    private String ssid24Ghz;

    @Column(name = "password_24ghz", nullable = false, length = 128)
    private String password24Ghz;

    @Column(name = "channel_24ghz", nullable = false, length = 16)
    @Builder.Default
    private String channel24Ghz = "AUTO";

    @Column(name = "ssid_5ghz", nullable = false, length = 32)
    private String ssid5Ghz;

    @Column(name = "password_5ghz", nullable = false, length = 128)
    private String password5Ghz;

    @Column(name = "channel_5ghz", nullable = false, length = 16)
    @Builder.Default
    private String channel5Ghz = "AUTO";

    @Column(name = "band_steering_enabled", nullable = false)
    @Builder.Default
    private boolean bandSteeringEnabled = true;

    @Column(name = "security_mode", nullable = false, length = 32)
    @Builder.Default
    private String securityMode = "WPA2-PSK/WPA3-SAE";

    @Column(name = "acs_sync_status", nullable = false, length = 32)
    @Builder.Default
    private String acsSyncStatus = "SYNCED_EMBEDDED_ACS";

    @Column(name = "last_inform_at")
    private OffsetDateTime lastInformAt;

    @Column(name = "last_reboot_at")
    private OffsetDateTime lastRebootAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.lastInformAt == null) {
            this.lastInformAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
