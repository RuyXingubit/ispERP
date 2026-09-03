package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.CompleteWorkOrderRequest;
import br.dev.xb.isperp.api.dto.ScheduleWorkOrderRequest;
import br.dev.xb.isperp.api.dto.WorkOrderResponse;
import br.dev.xb.isperp.api.dto.WorkOrderStatus;
import br.dev.xb.isperp.api.dto.WorkOrderType;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.mapper.WorkOrderMapper;
import br.dev.xb.isperp.service.WorkOrderService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WorkOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private WorkOrderService workOrderService;

    @MockitoBean
    private WorkOrderMapper workOrderMapper;

    private UUID workOrderId;
    private UUID contractId;
    private UUID customerId;
    private WorkOrder workOrder;
    private WorkOrderResponse workOrderResponse;

    @BeforeEach
    void setUp() {
        workOrderId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();

        workOrder = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                .type(WorkOrder.WorkOrderType.INSTALACAO)
                .build();

        workOrderResponse = new WorkOrderResponse();
        workOrderResponse.setId(workOrderId);
        workOrderResponse.setContractId(contractId);
        workOrderResponse.setCustomerId(customerId);
        workOrderResponse.setStatus(WorkOrderStatus.PENDING_SCHEDULE);
        workOrderResponse.setType(WorkOrderType.INSTALACAO);
    }

    @Test
    @DisplayName("GET /work-orders - Deve retornar lista de ordens de serviço conformes com o contrato")
    void shouldReturnAllWorkOrders() throws Exception {
        when(workOrderService.getAllWorkOrders()).thenReturn(List.of(workOrder));
        when(workOrderMapper.toResponseList(any())).thenReturn(List.of(workOrderResponse));

        mockMvc.perform(get("/work-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workOrderId.toString()))
                .andExpect(jsonPath("$[0].status").value("PENDING_SCHEDULE"))
                .andExpect(jsonPath("$[0].type").value("INSTALACAO"));
    }

    @Test
    @DisplayName("GET /work-orders/{id} - Deve retornar ordem de serviço por ID")
    void shouldReturnWorkOrderById() throws Exception {
        when(workOrderService.getWorkOrderById(workOrderId)).thenReturn(Optional.of(workOrder));
        when(workOrderMapper.toResponse(workOrder)).thenReturn(workOrderResponse);

        mockMvc.perform(get("/work-orders/{id}", workOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_SCHEDULE"));
    }

    @Test
    @DisplayName("POST /work-orders/{id}/schedule - Deve agendar O.S. com sucesso (contrato POST)")
    void shouldScheduleWorkOrderSuccessfully() throws Exception {
        ScheduleWorkOrderRequest request = new ScheduleWorkOrderRequest();
        request.setScheduledDate(LocalDate.now().plusDays(2));
        request.setScheduledPeriod("MANHA");
        request.setTechnicianName("Carlos Silva");

        workOrderResponse.setStatus(WorkOrderStatus.SCHEDULED);
        workOrderResponse.setTechnicianName("Carlos Silva");

        when(workOrderService.scheduleWorkOrder(eq(workOrderId), any())).thenReturn(workOrder);
        when(workOrderMapper.toResponse(any())).thenReturn(workOrderResponse);

        mockMvc.perform(post("/work-orders/{id}/schedule", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.technicianName").value("Carlos Silva"));
    }

    @Test
    @DisplayName("POST /work-orders/{id}/complete - Deve concluir O.S. com evidências de campo")
    void shouldCompleteWorkOrderSuccessfully() throws Exception {
        CompleteWorkOrderRequest request = new CompleteWorkOrderRequest();
        request.setOnuMac("48:57:02:11:22:33");
        request.setOnuSerial("ZTEGC89A12B3");
        request.setFiberSignalDbm(-19.50);

        workOrderResponse.setStatus(WorkOrderStatus.COMPLETED);
        workOrderResponse.setOnuSerial("ZTEGC89A12B3");

        when(workOrderService.completeWorkOrder(eq(workOrderId), any())).thenReturn(workOrder);
        when(workOrderMapper.toResponse(any())).thenReturn(workOrderResponse);

        mockMvc.perform(post("/work-orders/{id}/complete", workOrderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.onuSerial").value("ZTEGC89A12B3"));
    }
}
