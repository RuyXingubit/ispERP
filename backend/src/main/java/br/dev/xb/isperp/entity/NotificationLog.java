package br.dev.xb.isperp.entity;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel; // WHATSAPP, EMAIL, SMS

    @Column(name = "destination", nullable = false, length = 100)
    private String destination;

    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType; // INVOICE_PIX, PAYMENT_CONFIRMATION, WELCOME_CREDENTIALS, PLAN_UPGRADED

    @Column(name = "status", nullable = false, length = 30)
    private String status; // SENT, FAILED

    @Column(name = "external_id", length = 100)
    private String externalId;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

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
