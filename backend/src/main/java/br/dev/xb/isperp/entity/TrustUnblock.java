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
@Table(name = "trust_unblocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustUnblock {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do contrato é obrigatório")
    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "unblock_type", length = 30)
    @Builder.Default
    private String unblockType = "BOT_AUTO"; // BOT_AUTO, ATTENDANT_MANUAL

    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, CANCELLED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        if (this.requestedAt == null) {
            this.requestedAt = LocalDateTime.now();
        }
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusHours(48); // 48 horas de tolerância
        }
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "ACTIVE";
        }
    }
}
