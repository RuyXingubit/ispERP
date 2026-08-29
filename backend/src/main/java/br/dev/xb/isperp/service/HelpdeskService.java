package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.HelpdeskTicket;
import br.dev.xb.isperp.entity.TicketInteraction;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.HelpdeskTicketRepository;
import br.dev.xb.isperp.repository.TicketInteractionRepository;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.util.AnatelProtocolGenerator;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelpdeskService {

    private final HelpdeskTicketRepository ticketRepository;
    private final TicketInteractionRepository interactionRepository;
    private final WorkOrderRepository workOrderRepository;
    private final AnatelProtocolGenerator protocolGenerator;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public HelpdeskTicket createTicket(CreateTicketRequest req) {
        String protocol = protocolGenerator.generateProtocol();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slaDeadline = calculateSla(req.getCategory(), now);

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .protocol(protocol)
                .customerId(req.getCustomerId())
                .contractId(req.getContractId())
                .category(req.getCategory())
                .priority(req.getPriority() != null ? req.getPriority() : HelpdeskTicket.TicketPriority.NORMAL)
                .status(HelpdeskTicket.TicketStatus.OPEN)
                .channel(req.getChannel() != null ? req.getChannel() : HelpdeskTicket.TicketChannel.PHONE)
                .subject(req.getSubject())
                .description(req.getDescription())
                .assignedToUserId(req.getAttendantUserId())
                .slaDeadline(slaDeadline)
                .createdAt(now)
                .updatedAt(now)
                .build();

        HelpdeskTicket saved = ticketRepository.save(ticket);

        // Cria a primeira interação (descrição inicial do chamado)
        TicketInteraction firstInteraction = TicketInteraction.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .ticketId(saved.getId())
                .userId(req.getAttendantUserId())
                .senderType(TicketInteraction.SenderType.ATTENDANT)
                .senderName(req.getAttendantName() != null ? req.getAttendantName() : "Atendente")
                .message(req.getDescription())
                .isInternalNote(false)
                .createdAt(now)
                .build();
        interactionRepository.save(firstInteraction);

        log.info("Chamado ANATEL criado com sucesso. Protocolo: {}, Cliente: {}", protocol, req.getCustomerId());
        return saved;
    }

    @Transactional
    public HelpdeskTicket escalateToN2(UUID ticketId, UUID attendantUserId, String attendantName, String escalationReason) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + ticketId));

        ticket.setStatus(HelpdeskTicket.TicketStatus.IN_PROGRESS);
        ticket.setUpdatedAt(LocalDateTime.now());
        HelpdeskTicket updated = ticketRepository.save(ticket);

        // Registro de nota interna com a razão do escalonamento
        TicketInteraction interaction = TicketInteraction.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .ticketId(ticketId)
                .userId(attendantUserId)
                .senderType(TicketInteraction.SenderType.ATTENDANT)
                .senderName(attendantName != null ? attendantName : "Atendente N1")
                .message("Chamado escalonado para Suporte N2. Motivo: " + escalationReason)
                .isInternalNote(true)
                .createdAt(LocalDateTime.now())
                .build();
        interactionRepository.save(interaction);

        // Disparo de Evento de Domínio em tempo real para o Suporte Nível 2
        domainEventPublisher.publish(GenericDomainEvent.builder()
                .eventType("HELPDESK_TICKET_ESCALATED_N2")
                .aggregateId(ticket.getId().toString())
                .aggregateType("HelpdeskTicket")
                .payload(Map.of(
                        "ticketId", ticket.getId().toString(),
                        "protocol", ticket.getProtocol(),
                        "customerId", ticket.getCustomerId().toString(),
                        "category", ticket.getCategory().name(),
                        "priority", ticket.getPriority().name(),
                        "reason", escalationReason
                ))
                .build());

        log.info("Chamado {} escalonado para Suporte Nível 2 com sucesso.", ticket.getProtocol());
        return updated;
    }

    @Transactional
    public HelpdeskTicket resolveByN2(UUID ticketId, UUID n2UserId, String n2Name, String resolutionNotes) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + ticketId));

        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(HelpdeskTicket.TicketStatus.RESOLVED);
        ticket.setResolvedAt(now);
        ticket.setResolutionNotes(resolutionNotes);
        ticket.setUpdatedAt(now);
        HelpdeskTicket updated = ticketRepository.save(ticket);

        TicketInteraction interaction = TicketInteraction.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .ticketId(ticketId)
                .userId(n2UserId)
                .senderType(TicketInteraction.SenderType.SUPPORT_N2)
                .senderName(n2Name != null ? n2Name : "Suporte N2")
                .message("Chamado resolvido logicamente pelo Suporte N2: " + resolutionNotes)
                .isInternalNote(false)
                .createdAt(now)
                .build();
        interactionRepository.save(interaction);

        log.info("Chamado {} resolvido pelo Suporte N2.", ticket.getProtocol());
        return updated;
    }

    @Transactional
    public WorkOrder escalateToWorkOrder(UUID ticketId, UUID n2UserId, String n2Name, String technicalReason) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + ticketId));

        LocalDateTime now = LocalDateTime.now();

        // Criação da Ordem de Serviço de Campo
        WorkOrder workOrder = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(ticket.getCustomerId())
                .contractId(ticket.getContractId() != null ? ticket.getContractId() : ticket.getCustomerId())
                .type(WorkOrder.WorkOrderType.MANUTENCAO)
                .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                .notes("Origem Chamado ANATEL [" + ticket.getProtocol() + "]: " + technicalReason)
                .createdAt(now)
                .updatedAt(now)
                .build();

        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);

        // Vincula a O.S. ao chamado e muda status para aguardando visita de campo
        ticket.setWorkOrderId(savedWorkOrder.getId());
        ticket.setStatus(HelpdeskTicket.TicketStatus.WAITING_FIELD_VISIT);
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);

        // Registra histórico interno
        TicketInteraction interaction = TicketInteraction.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .ticketId(ticketId)
                .userId(n2UserId)
                .senderType(TicketInteraction.SenderType.SUPPORT_N2)
                .senderName(n2Name != null ? n2Name : "Suporte N2")
                .message("Necessária visita técnica de campo. O.S. gerada #" + savedWorkOrder.getId().toString().substring(0, 8) + ". Motivo: " + technicalReason)
                .isInternalNote(true)
                .createdAt(now)
                .build();
        interactionRepository.save(interaction);

        // Dispara evento para o Analista de Suporte agendar e roteirizar via GeoCEP
        domainEventPublisher.publish(GenericDomainEvent.builder()
                .eventType("WORK_ORDER_CREATED_FROM_TICKET")
                .aggregateId(savedWorkOrder.getId().toString())
                .aggregateType("WorkOrder")
                .payload(Map.of(
                        "workOrderId", savedWorkOrder.getId().toString(),
                        "ticketId", ticket.getId().toString(),
                        "protocol", ticket.getProtocol(),
                        "customerId", ticket.getCustomerId().toString(),
                        "reason", technicalReason
                ))
                .build());

        log.info("Chamado {} transformado em O.S. {} pelo Suporte N2.", ticket.getProtocol(), savedWorkOrder.getId());
        return savedWorkOrder;
    }

    @Transactional
    public TicketInteraction addInteraction(UUID ticketId, AddInteractionRequest req) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + ticketId));

        TicketInteraction interaction = TicketInteraction.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .ticketId(ticketId)
                .userId(req.getUserId())
                .senderType(req.getSenderType())
                .senderName(req.getSenderName())
                .message(req.getMessage())
                .isInternalNote(req.getIsInternalNote() != null ? req.getIsInternalNote() : false)
                .attachmentUrl(req.getAttachmentUrl())
                .createdAt(LocalDateTime.now())
                .build();

        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        return interactionRepository.save(interaction);
    }

    @Transactional
    public HelpdeskTicket closeTicket(UUID ticketId, Integer satisfactionRating, String closureNotes) {
        HelpdeskTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Chamado não encontrado: " + ticketId));

        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(HelpdeskTicket.TicketStatus.CLOSED);
        ticket.setClosedAt(now);
        ticket.setAnatelSatisfactionRating(satisfactionRating);
        if (closureNotes != null) {
            ticket.setResolutionNotes(closureNotes);
        }
        ticket.setUpdatedAt(now);
        return ticketRepository.save(ticket);
    }

    public List<HelpdeskTicket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Optional<HelpdeskTicket> getTicketById(UUID id) {
        return ticketRepository.findById(id);
    }

    public Optional<HelpdeskTicket> getTicketByProtocol(String protocol) {
        return ticketRepository.findByProtocol(protocol);
    }

    public List<HelpdeskTicket> getTicketsByCustomer(UUID customerId) {
        return ticketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<TicketInteraction> getInteractions(UUID ticketId, boolean includeInternal) {
        if (includeInternal) {
            return interactionRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        } else {
            return interactionRepository.findByTicketIdAndIsInternalNoteFalseOrderByCreatedAtAsc(ticketId);
        }
    }

    private LocalDateTime calculateSla(HelpdeskTicket.TicketCategory category, LocalDateTime from) {
        if (category == null) return from.plusHours(48);
        return switch (category) {
            case CONNECTION_OUTAGE, FINANCIAL, CANCELLATION_REQUEST -> from.plusHours(24);
            case SLOW_SPEED, ROUTER_CONFIG, OTHER -> from.plusHours(48);
            case ADDRESS_CHANGE, ROOM_TRANSFER -> from.plusHours(72);
        };
    }

    @Data
    @Builder
    public static class CreateTicketRequest {
        private UUID customerId;
        private UUID contractId;
        private HelpdeskTicket.TicketCategory category;
        private HelpdeskTicket.TicketPriority priority;
        private HelpdeskTicket.TicketChannel channel;
        private String subject;
        private String description;
        private UUID attendantUserId;
        private String attendantName;
    }

    @Data
    @Builder
    public static class AddInteractionRequest {
        private UUID userId;
        private TicketInteraction.SenderType senderType;
        private String senderName;
        private String message;
        private Boolean isInternalNote;
        private String attachmentUrl;
    }
}
