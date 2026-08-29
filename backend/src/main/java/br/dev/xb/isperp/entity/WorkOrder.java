package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@SuppressWarnings("null")
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

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_period", length = 30)
    private String scheduledPeriod; // MANHA, TARDE, NOITE, SABADO_MANHA

    @Column(name = "technician_name", length = 150)
    private String technicianName;

    @Column(name = "onu_mac", length = 50)
    private String onuMac;

    @Column(name = "onu_serial", length = 50)
    private String onuSerial;

    @Column(name = "fiber_signal_dbm", precision = 5, scale = 2)
    private BigDecimal fiberSignalDbm;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

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
