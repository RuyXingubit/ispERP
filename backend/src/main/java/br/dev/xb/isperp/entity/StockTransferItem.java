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
@Table(name = "stock_transfer_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "Transferência é obrigatória")
    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "asset_id")
    private UUID assetId; // Para itens serializados (ONT, Roteador, etc)

    @Column(name = "item_id")
    private UUID itemId; // Para itens a granel (Cabo, Conector, etc)

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
    }
}
