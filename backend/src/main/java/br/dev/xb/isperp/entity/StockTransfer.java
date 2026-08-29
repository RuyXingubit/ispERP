package br.dev.xb.isperp.entity;

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
@Table(name = "stock_transfers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Código da transferência é obrigatório")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotNull(message = "Depósito de origem é obrigatório")
    @Column(name = "origin_warehouse_id", nullable = false)
    private UUID originWarehouseId;

    @NotNull(message = "Depósito de destino é obrigatório")
    @Column(name = "destination_warehouse_id", nullable = false)
    private UUID destinationWarehouseId;

    @Column(name = "carrier_user_id")
    private UUID carrierUserId; // Colaborador / Portador responsável

    @NotBlank(message = "Nome do portador/transportador é obrigatório")
    @Column(name = "carrier_name", nullable = false, length = 150)
    private String carrierName;

    @NotBlank(message = "Documento do portador é obrigatório")
    @Column(name = "carrier_document", nullable = false, length = 20)
    private String carrierDocument; // CPF ou CNPJ

    @Enumerated(EnumType.STRING)
    @Column(name = "carrier_type", nullable = false, length = 30)
    @Builder.Default
    private CarrierType carrierType = CarrierType.COLABORADOR;

    @Column(name = "dispatched_by_user_id")
    private UUID dispatchedByUserId;

    @Column(name = "received_by_user_id")
    private UUID receivedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "dispatch_photo_url", columnDefinition = "text")
    private String dispatchPhotoUrl;

    @Column(name = "receipt_photo_url", columnDefinition = "text")
    private String receiptPhotoUrl;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

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

    public enum CarrierType {
        COLABORADOR,
        TERCEIRO,
        TRANSPORTADORA
    }

    public enum TransferStatus {
        PENDING,
        IN_TRANSIT,
        RECEIVED,
        CANCELED
    }
}
