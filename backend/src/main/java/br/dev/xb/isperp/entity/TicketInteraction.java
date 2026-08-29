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
@Table(name = "ticket_interactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SuppressWarnings("null")
public class TicketInteraction {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "ID do chamado é obrigatório")
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "is_internal_note", nullable = false)
    @Builder.Default
    private Boolean isInternalNote = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private SenderType senderType;

    @NotBlank(message = "Nome do remetente é obrigatório")
    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @NotBlank(message = "Mensagem é obrigatória")
    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UuidCreatorUtils.generateUuidV7();
        }
        this.createdAt = LocalDateTime.now();
        if (this.isInternalNote == null) {
            this.isInternalNote = false;
        }
    }

    public enum SenderType {
        ATTENDANT,
        SUPPORT_N2,
        SUPPORT_ANALYST,
        CUSTOMER,
        SYSTEM_BOT
    }
}
