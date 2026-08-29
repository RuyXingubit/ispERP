package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "custody_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustodyLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Nullable
    @Column(name = "asset_id")
    private UUID assetId;

    @Nullable
    @Column(name = "item_id")
    private UUID itemId;

    @Nullable
    @Column(name = "from_user_id")
    private UUID fromUserId;

    @Nullable
    @Column(name = "to_user_id")
    private UUID toUserId;

    @Nullable
    @Column(name = "from_warehouse_id")
    private UUID fromWarehouseId;

    @Nullable
    @Column(name = "to_warehouse_id")
    private UUID toWarehouseId;

    @Nullable
    @Column(name = "work_order_id")
    private UUID workOrderId;

    @NotBlank(message = "Tipo de evento é obrigatório")
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Nullable
    @Column(name = "photo_url", columnDefinition = "text")
    private String photoUrl;

    @Nullable
    @Column(name = "notes", columnDefinition = "text")
    private String notes;

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
