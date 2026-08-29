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
@Table(name = "inventory_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Código do item é obrigatório")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank(message = "Nome do item é obrigatório")
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Categoria do item é obrigatória")
    @Column(name = "category", nullable = false, length = 50)
    private String category; // ONU_ONT, CABO_DROP, CONECTOR, PTO

    @NotNull(message = "Quantidade em estoque é obrigatória")
    @Column(name = "quantity_in_stock", nullable = false)
    @Builder.Default
    private Integer quantityInStock = 0;

    @NotNull(message = "Quantidade mínima é obrigatória")
    @Column(name = "min_quantity", nullable = false)
    @Builder.Default
    private Integer minQuantity = 10;

    @Column(name = "unit", nullable = false, length = 20)
    @Builder.Default
    private String unit = "UN";

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
