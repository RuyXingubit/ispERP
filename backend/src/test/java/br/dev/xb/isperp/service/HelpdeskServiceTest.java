package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.HelpdeskTicket;
import br.dev.xb.isperp.entity.TicketInteraction;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.repository.HelpdeskTicketRepository;
import br.dev.xb.isperp.repository.TicketInteractionRepository;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.util.AnatelProtocolGenerator;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelpdeskServiceTest {

    @Mock
    private HelpdeskTicketRepository ticketRepository;

    @Mock
    private TicketInteractionRepository interactionRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private AnatelProtocolGenerator protocolGenerator;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private HelpdeskService helpdeskService;

    private UUID customerId;
    private UUID contractId;
    private UUID ticketId;

    @BeforeEach
    void setUp() {
        customerId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        ticketId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve abrir chamado com protocolo ANATEL e calcular SLA de 24h para Sem Conexão")
    void shouldCreateTicketWithAnatelProtocolAndSla() {
        when(protocolGenerator.generateProtocol()).thenReturn("20260829-12345");
        when(ticketRepository.save(any(HelpdeskTicket.class))).thenAnswer(i -> i.getArgument(0));

        HelpdeskService.CreateTicketRequest request = HelpdeskService.CreateTicketRequest.builder()
                .customerId(customerId)
                .contractId(contractId)
                .category(HelpdeskTicket.TicketCategory.CONNECTION_OUTAGE)
                .subject("Cliente sem internet")
                .description("LOS piscando em vermelho na ONU")
                .attendantUserId(UuidCreatorUtils.generateUuidV7())
                .attendantName("Atendente Maria")
                .build();

        HelpdeskTicket created = helpdeskService.createTicket(request);

        assertNotNull(created);
        assertEquals("20260829-12345", created.getProtocol());
        assertEquals(HelpdeskTicket.TicketStatus.OPEN, created.getStatus());
        assertTrue(created.getSlaDeadline().isAfter(LocalDateTime.now().plusHours(23)));
        verify(interactionRepository, times(1)).save(any(TicketInteraction.class));
    }

    @Test
    @DisplayName("Deve escalonar chamado para Suporte N2 e disparar evento de domínio em tempo real")
    void shouldEscalateTicketToN2AndPublishEvent() {
        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("20260829-12345")
                .customerId(customerId)
                .category(HelpdeskTicket.TicketCategory.SLOW_SPEED)
                .status(HelpdeskTicket.TicketStatus.OPEN)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(HelpdeskTicket.class))).thenAnswer(i -> i.getArgument(0));

        HelpdeskTicket updated = helpdeskService.escalateToN2(
                ticketId, UuidCreatorUtils.generateUuidV7(), "Atendente Maria", "Lentidão persistente após reboot");

        assertEquals(HelpdeskTicket.TicketStatus.IN_PROGRESS, updated.getStatus());
        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
        verify(interactionRepository, times(1)).save(any(TicketInteraction.class));
    }

    @Test
    @DisplayName("Deve transformar chamado em Ordem de Serviço pelo Suporte N2 quando houver falha física de campo")
    void shouldTransformTicketIntoWorkOrder() {
        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("20260829-12345")
                .customerId(customerId)
                .contractId(contractId)
                .category(HelpdeskTicket.TicketCategory.CONNECTION_OUTAGE)
                .status(HelpdeskTicket.TicketStatus.IN_PROGRESS)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> {
            WorkOrder wo = i.getArgument(0);
            wo.setId(UuidCreatorUtils.generateUuidV7());
            return wo;
        });
        when(ticketRepository.save(any(HelpdeskTicket.class))).thenAnswer(i -> i.getArgument(0));

        WorkOrder wo = helpdeskService.escalateToWorkOrder(
                ticketId, UuidCreatorUtils.generateUuidV7(), "N2 Roberto", "Fibra rompida no poste em frente à residência");

        assertNotNull(wo);
        assertEquals(WorkOrder.WorkOrderType.MANUTENCAO, wo.getType());
        assertEquals(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE, wo.getStatus());
        assertEquals(HelpdeskTicket.TicketStatus.WAITING_FIELD_VISIT, ticket.getStatus());
        assertNotNull(ticket.getWorkOrderId());
        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve fechar chamado com avaliação de satisfação ANATEL (1..5)")
    void shouldCloseTicketWithSatisfactionRating() {
        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("20260829-12345")
                .customerId(customerId)
                .status(HelpdeskTicket.TicketStatus.RESOLVED)
                .build();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(HelpdeskTicket.class))).thenAnswer(i -> i.getArgument(0));

        HelpdeskTicket closed = helpdeskService.closeTicket(ticketId, 5, "Cliente atendido e satisfeito.");

        assertEquals(HelpdeskTicket.TicketStatus.CLOSED, closed.getStatus());
        assertEquals(5, closed.getAnatelSatisfactionRating());
        assertNotNull(closed.getClosedAt());
    }
}
