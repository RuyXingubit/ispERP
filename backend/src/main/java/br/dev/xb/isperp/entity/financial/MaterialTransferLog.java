package br.dev.xb.isperp.entity.financial;

import br.dev.xb.isperp.entity.EntityLifecycleListener;
import br.dev.xb.isperp.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@EntityListeners(EntityLifecycleListener.class)
@Table(name = "material_transfer_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialTransferLog implements CustodyTransferLogEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_custody_id", nullable = false)
    private UserMaterialCustody materialCustody;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private CashTransferStatus status = CashTransferStatus.PENDING_ACCEPTANCE;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;
    @Override
    public void initEntityDefaults() {
        if (this.quantity == null) {
            this.quantity = BigDecimal.ONE;
        }
    }
}
