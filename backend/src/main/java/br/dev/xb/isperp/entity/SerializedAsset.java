package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "serialized_assets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerializedAsset {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Nullable
    @Column(name = "mac_address", length = 50)
    private String macAddress;

    @NotBlank(message = "Número de série é obrigatório")
    @Column(name = "serial_number", nullable = false, unique = true, length = 100)
    private String serialNumber;

    @NotBlank(message = "Marca / Modelo é obrigatório")
    @Column(name = "brand_model", nullable = false, length = 150)
    private String brandModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private AssetCategory category;

    @Column(name = "replacement_value", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal replacementValue = BigDecimal.ZERO;

    @Nullable
    @Column(name = "current_warehouse_id")
    private UUID currentWarehouseId;

    @Nullable
    @Column(name = "current_holder_user_id")
    private UUID currentHolderUserId; // CPF/Pessoa Física com a custódia

    @Nullable
    @Column(name = "current_customer_id")
    private UUID currentCustomerId;

    @Nullable
    @Column(name = "current_contract_id")
    private UUID currentContractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AssetStatus status = AssetStatus.DISPONIVEL_DEPOSITO;

    @Nullable
    @Column(name = "last_movement_at")
    private LocalDateTime lastMovementAt;

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
        this.lastMovementAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum AssetCategory {
        ONU_ONT,
        ROUTER_MESH,
        TOOL_FUSION_MACHINE,
        TOOL_OTDR,
        TOOL_POWER_METER,
        TOOL_CLEAVER,
        SWITCH,
        OLT
    }

    public enum AssetStatus {
        DISPONIVEL_DEPOSITO,
        CUSTODIA_COLABORADOR,
        EM_TRANSITO,
        INSTALADO_CLIENTE,
        RETIRADO_PENDENTE_DEVOLUCAO,
        DEFEITO_TRIAGEM
    }
}
