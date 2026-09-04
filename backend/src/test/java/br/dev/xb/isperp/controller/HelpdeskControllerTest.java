package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.HelpdeskTicket;
import br.dev.xb.isperp.entity.TicketInteraction;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.mapper.HelpdeskMapperImpl;
import br.dev.xb.isperp.mapper.WorkOrderMapperImpl;
import br.dev.xb.isperp.service.HelpdeskService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelpdeskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({HelpdeskMapperImpl.class, WorkOrderMapperImpl.class})
@SuppressWarnings("null")
class HelpdeskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HelpdeskService helpdeskService;

    @Test
    @DisplayName("GET /helpdesk/tickets - Deve listar chamados mapeados em DTOs")
    void testGetAllTickets() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID customerId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090400001")
                .customerId(customerId)
                .category(HelpdeskTicket.TicketCategory.CONNECTION_OUTAGE)
                .priority(HelpdeskTicket.TicketPriority.HIGH)
                .status(HelpdeskTicket.TicketStatus.OPEN)
                .channel(HelpdeskTicket.TicketChannel.PHONE)
                .subject("Sem conexão")
                .description("LED PON piscando vermelho")
                .slaDeadline(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.getAllTickets()).thenReturn(List.of(ticket));

        mockMvc.perform(get("/helpdesk/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ticketId.toString()))
                .andExpect(jsonPath("$[0].protocol").value("2026090400001"))
                .andExpect(jsonPath("$[0].category").value("CONNECTION_OUTAGE"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /helpdesk/tickets - Deve abrir chamado com sucesso")
    void testCreateTicket() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID customerId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090400002")
                .customerId(customerId)
                .category(HelpdeskTicket.TicketCategory.SLOW_SPEED)
                .priority(HelpdeskTicket.TicketPriority.NORMAL)
                .status(HelpdeskTicket.TicketStatus.OPEN)
                .channel(HelpdeskTicket.TicketChannel.WHATSAPP_BOT)
                .subject("Lentidão constante")
                .description("Velocidade abaixo de 10% do contratado")
                .slaDeadline(LocalDateTime.now().plusHours(48))
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.createTicket(any(HelpdeskService.CreateTicketRequest.class))).thenReturn(ticket);

        String json = """
                {
                    "customerId": "%s",
                    "category": "SLOW_SPEED",
                    "priority": "NORMAL",
                    "channel": "WHATSAPP_BOT",
                    "subject": "Lentidão constante",
                    "description": "Velocidade abaixo de 10%% do contratado"
                }
                """.formatted(customerId);

        mockMvc.perform(post("/helpdesk/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.protocol").value("2026090400002"))
                .andExpect(jsonPath("$.category").value("SLOW_SPEED"));
    }

    @Test
    @DisplayName("GET /helpdesk/tickets/{id} - Sucesso")
    void testGetTicketByIdSuccess() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID customerId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090400003")
                .customerId(customerId)
                .category(HelpdeskTicket.TicketCategory.FINANCIAL)
                .priority(HelpdeskTicket.TicketPriority.NORMAL)
                .status(HelpdeskTicket.TicketStatus.RESOLVED)
                .channel(HelpdeskTicket.TicketChannel.PORTAL)
                .subject("Dúvida sobre fatura")
                .description("Cliente solicitou 2a via")
                .slaDeadline(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.getTicketById(ticketId)).thenReturn(Optional.of(ticket));

        mockMvc.perform(get("/helpdesk/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.protocol").value("2026090400003"));
    }

    @Test
    @DisplayName("GET /helpdesk/tickets/{id} - 404 quando não encontrado")
    void testGetTicketByIdNotFound() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        when(helpdeskService.getTicketById(ticketId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/helpdesk/tickets/{id}", ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /helpdesk/tickets/protocol/{protocol} - Sucesso")
    void testGetTicketByProtocolSuccess() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID customerId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090499999")
                .customerId(customerId)
                .category(HelpdeskTicket.TicketCategory.OTHER)
                .priority(HelpdeskTicket.TicketPriority.LOW)
                .status(HelpdeskTicket.TicketStatus.OPEN)
                .channel(HelpdeskTicket.TicketChannel.EMAIL)
                .subject("Troca de senha Wi-Fi")
                .description("Configuração de SSID")
                .slaDeadline(LocalDateTime.now().plusHours(48))
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.getTicketByProtocol("2026090499999")).thenReturn(Optional.of(ticket));

        mockMvc.perform(get("/helpdesk/tickets/protocol/{protocol}", "2026090499999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.protocol").value("2026090499999"));
    }

    @Test
    @DisplayName("GET /helpdesk/tickets/customer/{customerId} - Sucesso")
    void testGetTicketsByCustomer() throws Exception {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        UUID ticketId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090400010")
                .customerId(customerId)
                .category(HelpdeskTicket.TicketCategory.ROUTER_CONFIG)
                .priority(HelpdeskTicket.TicketPriority.NORMAL)
                .status(HelpdeskTicket.TicketStatus.IN_PROGRESS)
                .channel(HelpdeskTicket.TicketChannel.PHONE)
                .subject("Redirecionamento de portas")
                .description("Port forwarding para câmeras")
                .slaDeadline(LocalDateTime.now().plusHours(48))
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.getTicketsByCustomer(customerId)).thenReturn(List.of(ticket));

        mockMvc.perform(get("/helpdesk/tickets/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ticketId.toString()))
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));
    }

    @Test
    @DisplayName("POST /helpdesk/tickets/{id}/escalate-n2 - Sucesso")
    void testEscalateToN2() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID attendantId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090400011")
                .customerId(UuidCreatorUtils.generateUuidV7())
                .category(HelpdeskTicket.TicketCategory.CONNECTION_OUTAGE)
                .priority(HelpdeskTicket.TicketPriority.URGENT)
                .status(HelpdeskTicket.TicketStatus.IN_PROGRESS)
                .channel(HelpdeskTicket.TicketChannel.PHONE)
                .subject("Falha física na fibra")
                .description("Problema persiste após reboot")
                .slaDeadline(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.escalateToN2(eq(ticketId), any(), any(), any())).thenReturn(ticket);

        String json = """
                {
                    "attendantUserId": "%s",
                    "attendantName": "Mariana N1",
                    "reason": "Testes N1 falharam"
                }
                """.formatted(attendantId);

        mockMvc.perform(post("/helpdesk/tickets/{id}/escalate-n2", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("POST /helpdesk/tickets/{id}/resolve-n2 - Sucesso")
    void testResolveByN2() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID n2UserId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090400012")
                .customerId(UuidCreatorUtils.generateUuidV7())
                .category(HelpdeskTicket.TicketCategory.ROUTER_CONFIG)
                .priority(HelpdeskTicket.TicketPriority.NORMAL)
                .status(HelpdeskTicket.TicketStatus.RESOLVED)
                .channel(HelpdeskTicket.TicketChannel.PHONE)
                .subject("Configuração de VLAN")
                .description("VLAN ajustada")
                .slaDeadline(LocalDateTime.now().plusHours(48))
                .resolutionNotes("VLAN 100 recriada na OLT")
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.resolveByN2(eq(ticketId), any(), any(), any())).thenReturn(ticket);

        String json = """
                {
                    "n2UserId": "%s",
                    "n2Name": "Especialista NOC",
                    "resolutionNotes": "VLAN 100 recriada na OLT"
                }
                """.formatted(n2UserId);

        mockMvc.perform(post("/helpdesk/tickets/{id}/resolve-n2", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("POST /helpdesk/tickets/{id}/escalate-work-order - Deve criar WorkOrder")
    void testEscalateToWorkOrder() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID workOrderId = UuidCreatorUtils.generateUuidV7();
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        UUID customerId = UuidCreatorUtils.generateUuidV7();

        WorkOrder workOrder = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .type(WorkOrder.WorkOrderType.MANUTENCAO)
                .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                .scheduledDate(LocalDate.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.escalateToWorkOrder(eq(ticketId), any(), any(), any())).thenReturn(workOrder);

        String json = """
                {
                    "technicalReason": "Drop óptico rompido na entrada da casa"
                }
                """;

        mockMvc.perform(post("/helpdesk/tickets/{id}/escalate-work-order", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(workOrderId.toString()))
                .andExpect(jsonPath("$.type").value("MANUTENCAO"));
    }

    @Test
    @DisplayName("POST /helpdesk/tickets/{id}/interactions - Deve adicionar interação")
    void testAddInteraction() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID interactionId = UuidCreatorUtils.generateUuidV7();

        TicketInteraction interaction = TicketInteraction.builder()
                .id(interactionId)
                .ticketId(ticketId)
                .senderType(TicketInteraction.SenderType.ATTENDANT)
                .senderName("Atendente Júlia")
                .message("Orientado cliente a realizar reset físico na ONU")
                .isInternalNote(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.addInteraction(eq(ticketId), any())).thenReturn(interaction);

        String json = """
                {
                    "senderType": "ATTENDANT",
                    "senderName": "Atendente Júlia",
                    "message": "Orientado cliente a realizar reset físico na ONU",
                    "isInternalNote": false
                }
                """;

        mockMvc.perform(post("/helpdesk/tickets/{id}/interactions", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(interactionId.toString()))
                .andExpect(jsonPath("$.senderName").value("Atendente Júlia"))
                .andExpect(jsonPath("$.isInternalNote").value(false));
    }

    @Test
    @DisplayName("GET /helpdesk/tickets/{id}/interactions - Deve listar interações")
    void testGetInteractions() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();
        UUID interactionId = UuidCreatorUtils.generateUuidV7();

        TicketInteraction interaction = TicketInteraction.builder()
                .id(interactionId)
                .ticketId(ticketId)
                .senderType(TicketInteraction.SenderType.CUSTOMER)
                .senderName("José Oliveira")
                .message("Obrigado, voltou a funcionar.")
                .isInternalNote(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.getInteractions(eq(ticketId), eq(true))).thenReturn(List.of(interaction));

        mockMvc.perform(get("/helpdesk/tickets/{id}/interactions", ticketId)
                        .param("includeInternal", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(interactionId.toString()))
                .andExpect(jsonPath("$[0].senderType").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /helpdesk/tickets/{id}/close - Deve fechar chamado")
    void testCloseTicket() throws Exception {
        UUID ticketId = UuidCreatorUtils.generateUuidV7();

        HelpdeskTicket ticket = HelpdeskTicket.builder()
                .id(ticketId)
                .protocol("2026090400099")
                .customerId(UuidCreatorUtils.generateUuidV7())
                .category(HelpdeskTicket.TicketCategory.SLOW_SPEED)
                .priority(HelpdeskTicket.TicketPriority.LOW)
                .status(HelpdeskTicket.TicketStatus.CLOSED)
                .channel(HelpdeskTicket.TicketChannel.PHONE)
                .subject("Finalizado")
                .description("Desc")
                .anatelSatisfactionRating(5)
                .resolutionNotes("Conexão normalizada")
                .slaDeadline(LocalDateTime.now())
                .closedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(helpdeskService.closeTicket(eq(ticketId), any(), any())).thenReturn(ticket);

        String json = """
                {
                    "satisfactionRating": 5,
                    "closureNotes": "Conexão normalizada"
                }
                """;

        mockMvc.perform(post("/helpdesk/tickets/{id}/close", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.anatelSatisfactionRating").value(5));
    }
}
