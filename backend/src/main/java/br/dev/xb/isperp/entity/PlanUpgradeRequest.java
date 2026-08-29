package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_upgrade_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanUpgradeRequest {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do contrato é obrigatório")
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "old_plan_id")
    private UUID oldPlanId;

    @NotNull(message = "ID do novo plano é obrigatório")
    @Column(name = "new_plan_id", nullable = false)
    private UUID newPlanId;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "COMPLETED"; // REQUESTED, COMPLETED, REJECTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "COMPLETED";
        }
    }
}
