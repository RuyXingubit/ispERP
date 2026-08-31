package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do contrato é obrigatório")
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @NotNull(message = "ID do cliente é obrigatório")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    @Builder.Default
    private WorkOrderType type = WorkOrderType.INSTALACAO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.PENDING_SCHEDULE;

    @Nullable
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Nullable
    @Column(name = "scheduled_period", length = 30)
    private String scheduledPeriod; // MANHA, TARDE, NOITE, SABADO_MANHA

    @Nullable
    @Column(name = "technician_name", length = 150)
    private String technicianName;

    @Nullable
    @Column(name = "onu_mac", length = 50)
    private String onuMac;

    @Nullable
    @Column(name = "onu_serial", length = 50)
    private String onuSerial;

    @Nullable
    @Column(name = "fiber_signal_dbm", precision = 5, scale = 2)
    private BigDecimal fiberSignalDbm;

    @Nullable
    @Column(name = "onu_rx_power_dbm", precision = 5, scale = 2)
    private BigDecimal onuRxPowerDbm;

    @Nullable
    @Column(name = "radius_authenticated")
    @Builder.Default
    private Boolean radiusAuthenticated = false;

    @Nullable
    @Column(name = "allocated_warehouse_id")
    private UUID allocatedWarehouseId;

    @Nullable
    @Column(name = "cto_id")
    private UUID ctoId;

    @Nullable
    @Column(name = "cto_port_number")
    private Integer ctoPortNumber;

    @Nullable
    @Column(name = "technician_latitude", precision = 10, scale = 8)
    private BigDecimal technicianLatitude;

    @Nullable
    @Column(name = "technician_longitude", precision = 11, scale = 8)
    private BigDecimal technicianLongitude;

    @Nullable
    @Column(name = "gps_captured_at")
    private LocalDateTime gpsCapturedAt;

    @Nullable
    @Column(name = "installation_photo_url", columnDefinition = "text")
    private String installationPhotoUrl;

    @Nullable
    @Column(name = "tool_agreement_id")
    private UUID toolAgreementId;

    @Nullable
    @Column(name = "digital_signature_base64", columnDefinition = "text")
    private String digitalSignatureBase64;

    @Nullable
    @Column(name = "customer_signature_name", length = 150)
    private String customerSignatureName;

    @Nullable
    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Nullable
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Nullable
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.type == null) {
            this.type = WorkOrderType.INSTALACAO;
        }
        if (this.status == null) {
            this.status = WorkOrderStatus.PENDING_SCHEDULE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum WorkOrderStatus {
        PENDING_SCHEDULE,
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELED
    }

    public enum WorkOrderType {
        INSTALACAO,
        MANUTENCAO,
        RETIRADA
    }
}
