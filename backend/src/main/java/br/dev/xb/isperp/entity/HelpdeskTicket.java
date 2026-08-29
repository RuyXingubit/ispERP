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
@Table(name = "helpdesk_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HelpdeskTicket {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Protocolo ANATEL é obrigatório")
    @Column(name = "protocol", nullable = false, unique = true, length = 32)
    private String protocol;

    @NotNull(message = "ID do cliente é obrigatório")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private TicketPriority priority = TicketPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    @Builder.Default
    private TicketChannel channel = TicketChannel.PHONE;

    @NotBlank(message = "Assunto é obrigatório")
    @Column(name = "subject", nullable = false)
    private String subject;

    @NotBlank(message = "Descrição é obrigatória")
    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    @Column(name = "work_order_id")
    private UUID workOrderId;

    @NotNull(message = "Prazo de SLA é obrigatório")
    @Column(name = "sla_deadline", nullable = false)
    private LocalDateTime slaDeadline;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @Column(name = "anatel_satisfaction_rating")
    private Integer anatelSatisfactionRating; // 1 a 5

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

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
        if (this.status == null) {
            this.status = TicketStatus.OPEN;
        }
        if (this.priority == null) {
            this.priority = TicketPriority.NORMAL;
        }
        if (this.channel == null) {
            this.channel = TicketChannel.PHONE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum TicketCategory {
        FINANCIAL,
        CONNECTION_OUTAGE,
        SLOW_SPEED,
        ROUTER_CONFIG,
        ADDRESS_CHANGE,
        ROOM_TRANSFER,
        CANCELLATION_REQUEST,
        OTHER
    }

    public enum TicketPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum TicketStatus {
        OPEN,
        IN_PROGRESS,
        WAITING_CUSTOMER,
        WAITING_FIELD_VISIT,
        RESOLVED,
        CLOSED,
        CANCELLED
    }

    public enum TicketChannel {
        WHATSAPP_BOT,
        PORTAL,
        PHONE,
        IN_PERSON,
        EMAIL
    }
}
