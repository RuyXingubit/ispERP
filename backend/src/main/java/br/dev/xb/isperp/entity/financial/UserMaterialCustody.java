package br.dev.xb.isperp.entity.financial;

import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_material_custodies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMaterialCustody {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private MaterialType itemType;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "mac_address", length = 50)
    private String macAddress;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit", nullable = false, length = 20)
    @Builder.Default
    private String unit = "UN";

    @Column(name = "allocated_at", nullable = false, updatable = false)
    private OffsetDateTime allocatedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.allocatedAt == null) {
            this.allocatedAt = OffsetDateTime.now();
        }
        if (this.quantity == null) {
            this.quantity = BigDecimal.ONE;
        }
        if (this.unit == null) {
            this.unit = "UN";
        }
    }
}
