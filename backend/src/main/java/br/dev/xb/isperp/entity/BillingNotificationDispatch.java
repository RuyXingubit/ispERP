package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "billing_notification_dispatches",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_billing_notif_invoice_stage_channel",
            columnNames = {"invoice_id", "stage", "channel"}
        )
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingNotificationDispatch {

    public enum Stage {
        D_MINUS_3,
        D_ZERO,
        D_PLUS_5_ANATEL_NOTICE,
        PAYMENT_CONFIRMED
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "customer_name", length = 200)
    private @Nullable String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false, length = 40)
    private Stage stage;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "recipient", nullable = false, length = 180)
    private String recipient;

    @Column(name = "message_preview", nullable = false, columnDefinition = "TEXT")
    private String messagePreview;

    @Column(name = "delivery_status", nullable = false, length = 30)
    @Builder.Default
    private String deliveryStatus = "SENT";

    @Column(name = "dispatched_at", nullable = false)
    private OffsetDateTime dispatchedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.dispatchedAt == null) {
            this.dispatchedAt = OffsetDateTime.now();
        }
        if (this.deliveryStatus == null) {
            this.deliveryStatus = "SENT";
        }
    }
}
