package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.WorkOrderResponse;
import br.dev.xb.isperp.api.dto.WorkOrderStatus;
import br.dev.xb.isperp.api.dto.WorkOrderType;
import br.dev.xb.isperp.dto.InstallationMaterialDemandResponse;
import br.dev.xb.isperp.entity.MaterialDemandStatus;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.mapper.WorkOrderMapper;
import br.dev.xb.isperp.service.InstallationDemandService;
import br.dev.xb.isperp.service.TechnicianDispatchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InstallationDispatchController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class InstallationDispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstallationDemandService demandService;

    @MockitoBean
    private TechnicianDispatchService dispatchService;

    @MockitoBean
    private WorkOrderMapper workOrderMapper;

    @Test
    @DisplayName("GET /dispatch/installations/demands deve listar demandas de instalação")
    void testListDemands() throws Exception {
        InstallationMaterialDemandResponse demand = InstallationMaterialDemandResponse.builder()
                .id(UUID.randomUUID())
                .workOrderId(UUID.randomUUID())
                .contractId(UUID.randomUUID())
                .customerName("Carlos Silva")
                .estimatedDropMeters(120)
                .onuModelRequired("HG8145V5")
                .fastConnectorsCount(2)
                .ptoRosetteCount(1)
                .status(MaterialDemandStatus.PENDING_ALLOCATION)
                .build();

        when(demandService.listPendingDemands()).thenReturn(List.of(demand));

        mockMvc.perform(get("/dispatch/installations/demands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Carlos Silva"))
                .andExpect(jsonPath("$[0].estimatedDropMeters").value(120));
    }

    @Test
    @DisplayName("POST /dispatch/installations/{workOrderId}/dispatch deve despachar O.S.")
    void testDispatchWorkOrder() throws Exception {
        UUID workOrderId = UUID.randomUUID();
        UUID techId = UUID.randomUUID();

        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .status(WorkOrder.WorkOrderStatus.SCHEDULED)
                .build();

        WorkOrderResponse woResp = new WorkOrderResponse();
        woResp.setId(workOrderId);
        woResp.setStatus(WorkOrderStatus.SCHEDULED);
        woResp.setType(WorkOrderType.INSTALACAO);

        when(dispatchService.dispatchWorkOrder(workOrderId, techId)).thenReturn(wo);
        when(workOrderMapper.toResponse(any())).thenReturn(woResp);

        mockMvc.perform(post("/dispatch/installations/" + workOrderId + "/dispatch")
                        .param("technicianId", techId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId.toString()));
    }
}
