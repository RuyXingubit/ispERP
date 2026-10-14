package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.dto.CustodyLogResponse;
import br.dev.xb.isperp.api.dto.InventoryItemResponse;
import br.dev.xb.isperp.api.dto.MaterialCheckinOsRequest;
import br.dev.xb.isperp.api.dto.MaterialCheckinResponse;
import br.dev.xb.isperp.api.dto.MaterialCheckoutOsRequest;
import br.dev.xb.isperp.api.dto.StockEntryRequest;
import br.dev.xb.isperp.api.dto.StockMovementResponse;
import br.dev.xb.isperp.api.dto.TechnicianExecutionCompleteRequest;
import br.dev.xb.isperp.api.dto.WorkOrderResponse;
import br.dev.xb.isperp.api.dto.WorkOrderStatus;
import br.dev.xb.isperp.entity.CustodyLog;
import br.dev.xb.isperp.entity.InventoryItem;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.mapper.InventoryMapper;
import br.dev.xb.isperp.mapper.WorkOrderMapper;
import br.dev.xb.isperp.service.AssetCustodyService;
import br.dev.xb.isperp.service.InventoryService;
import br.dev.xb.isperp.service.TechnicianFieldExecutionService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        InventoryController.class,
        AssetCustodyController.class,
        TechnicianFieldExecutionController.class
})
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class MigratedOpenApiControllersTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private AssetCustodyService assetCustodyService;

    @MockitoBean
    private TechnicianFieldExecutionService fieldExecutionService;

    @MockitoBean
    private InventoryMapper inventoryMapper;

    @MockitoBean
    private WorkOrderMapper workOrderMapper;

    @Test
    @DisplayName("POST /inventory/entry - Deve registrar entrada de estoque conforme contrato OpenAPI")
    void shouldRegisterStockEntry() throws Exception {
        UUID itemId = UuidCreatorUtils.generateUuidV7();
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();

        StockEntryRequest request = new StockEntryRequest(
                warehouseId, "ONT-HW-AX2", "ONT Huawei Wi-Fi 6", "ONU_ONT", 15, "UN");
        request.setNotes("Compra NF 2040");

        InventoryItem updatedItem = InventoryItem.builder()
                .id(itemId)
                .code("ONT-HW-AX2")
                .name("ONT Huawei Wi-Fi 6")
                .category("ONU_ONT")
                .quantityInStock(25)
                .minQuantity(5)
                .unit("UN")
                .build();

        InventoryItemResponse response = new InventoryItemResponse();
        response.setId(itemId);
        response.setCode("ONT-HW-AX2");
        response.setName("ONT Huawei Wi-Fi 6");
        response.setQuantityInStock(25);

        when(inventoryService.registerStockEntry(
                eq(warehouseId), eq("ONT-HW-AX2"), eq("ONT Huawei Wi-Fi 6"),
                eq("ONU_ONT"), eq(15), eq("UN"), eq("Compra NF 2040")))
                .thenReturn(updatedItem);
        when(inventoryMapper.toResponse(updatedItem)).thenReturn(response);

        mockMvc.perform(post("/inventory/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.quantityInStock").value(25));
    }

    @Test
    @DisplayName("GET /inventory/items/{itemId}/movements - Deve listar Kardex do item com DTO OpenAPI")
    void shouldListItemMovements() throws Exception {
        UUID itemId = UuidCreatorUtils.generateUuidV7();
        UUID movId = UuidCreatorUtils.generateUuidV7();

        br.dev.xb.isperp.dto.StockMovementResponse movement = br.dev.xb.isperp.dto.StockMovementResponse.builder()
                .id(movId)
                .eventType("STOCK_ENTRY")
                .eventDescription("Entrada de Lote")
                .quantity(10)
                .balanceAfter(10)
                .build();

        StockMovementResponse movResponse = new StockMovementResponse();
        movResponse.setId(movId);
        movResponse.setEventType("STOCK_ENTRY");
        movResponse.setEventDescription("Entrada de Lote");
        movResponse.setQuantity(10);
        movResponse.setBalanceAfter(10);

        when(inventoryService.getItemMovements(itemId)).thenReturn(List.of(movement));
        when(inventoryMapper.toStockMovementResponseList(any())).thenReturn(List.of(movResponse));

        mockMvc.perform(get("/inventory/items/{itemId}/movements", itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(movId.toString()))
                .andExpect(jsonPath("$[0].eventType").value("STOCK_ENTRY"))
                .andExpect(jsonPath("$[0].balanceAfter").value(10));
    }

    @Test
    @DisplayName("POST /inventory/custody/materials/checkout-os - Deve autorizar saída de material sem expor @Entity")
    void shouldCheckoutMaterialForWorkOrder() throws Exception {
        UUID itemId = UuidCreatorUtils.generateUuidV7();
        UUID techId = UuidCreatorUtils.generateUuidV7();
        UUID woId = UuidCreatorUtils.generateUuidV7();
        UUID logId = UuidCreatorUtils.generateUuidV7();

        MaterialCheckoutOsRequest request = new MaterialCheckoutOsRequest(woId, techId, 150);
        request.setItemCode("CABO-DROP-1FO");
        request.setNotes("Saída para instalação");

        CustodyLog log = CustodyLog.builder()
                .id(logId)
                .itemId(itemId)
                .toUserId(techId)
                .workOrderId(woId)
                .eventType("MATERIAL_OS_CHECKOUT")
                .quantity(150)
                .build();

        CustodyLogResponse logResponse = new CustodyLogResponse();
        logResponse.setId(logId);
        logResponse.setItemId(itemId);
        logResponse.setToUserId(techId);
        logResponse.setEventType("MATERIAL_OS_CHECKOUT");
        logResponse.setQuantity(150);

        var serviceReq = new br.dev.xb.isperp.dto.MaterialCheckoutOsRequest();
        when(inventoryMapper.toServiceRequest(any(MaterialCheckoutOsRequest.class))).thenReturn(serviceReq);
        when(assetCustodyService.checkoutMaterialForWorkOrder(serviceReq)).thenReturn(log);
        when(inventoryMapper.toResponse(log)).thenReturn(logResponse);

        mockMvc.perform(post("/inventory/custody/materials/checkout-os")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logId.toString()))
                .andExpect(jsonPath("$.eventType").value("MATERIAL_OS_CHECKOUT"))
                .andExpect(jsonPath("$.quantity").value(150));
    }

    @Test
    @DisplayName("POST /inventory/custody/materials/checkin-os - Deve confirmar devolução de material com DTO OpenAPI")
    void shouldCheckinMaterialForWorkOrder() throws Exception {
        UUID techId = UuidCreatorUtils.generateUuidV7();
        UUID woId = UuidCreatorUtils.generateUuidV7();
        UUID logId = UuidCreatorUtils.generateUuidV7();

        MaterialCheckinOsRequest request = new MaterialCheckinOsRequest(woId, techId, 150, 100, 50);
        request.setItemCode("CABO-DROP-1FO");

        var serviceReq = new br.dev.xb.isperp.dto.MaterialCheckinOsRequest();
        var serviceRes = br.dev.xb.isperp.dto.MaterialCheckinResponse.builder()
                .logId(logId)
                .workOrderId(woId)
                .technicianUserId(techId)
                .status("CONFORMANT")
                .hasDivergence(false)
                .expectedRemaining(50)
                .actualRemaining(50)
                .divergenceQuantity(0)
                .build();

        MaterialCheckinResponse apiRes = new MaterialCheckinResponse();
        apiRes.setLogId(logId);
        apiRes.setWorkOrderId(woId);
        apiRes.setTechnicianUserId(techId);
        apiRes.setStatus("CONFORMANT");
        apiRes.setHasDivergence(false);
        apiRes.setExpectedRemaining(50);
        apiRes.setActualRemaining(50);
        apiRes.setDivergenceQuantity(0);

        when(inventoryMapper.toServiceRequest(any(MaterialCheckinOsRequest.class))).thenReturn(serviceReq);
        when(assetCustodyService.checkinMaterialForWorkOrder(serviceReq)).thenReturn(serviceRes);
        when(inventoryMapper.toApiCheckinResponse(serviceRes)).thenReturn(apiRes);

        mockMvc.perform(post("/inventory/custody/materials/checkin-os")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFORMANT"))
                .andExpect(jsonPath("$.hasDivergence").value(false))
                .andExpect(jsonPath("$.actualRemaining").value(50));
    }

    @Test
    @DisplayName("POST /technician/execution/{workOrderId}/complete - Deve encerrar O.S. retornando WorkOrderResponse")
    void shouldCompleteInstallationInField() throws Exception {
        UUID woId = UuidCreatorUtils.generateUuidV7();

        TechnicianExecutionCompleteRequest request = new TechnicianExecutionCompleteRequest("ZTEGC89A12B3");
        request.setOnuMac("48:57:02:11:22:33");
        request.setFiberSignalDbm(-18.7);
        request.setNotes("Instalação concluída");

        WorkOrder wo = WorkOrder.builder()
                .id(woId)
                .status(WorkOrder.WorkOrderStatus.COMPLETED)
                .build();

        WorkOrderResponse woResponse = new WorkOrderResponse();
        woResponse.setId(woId);
        woResponse.setStatus(WorkOrderStatus.COMPLETED);

        var serviceReq = new br.dev.xb.isperp.dto.TechnicianExecutionCompleteRequest();
        serviceReq.setOnuSerial("ZTEGC89A12B3");
        serviceReq.setOnuMac("48:57:02:11:22:33");
        serviceReq.setFiberSignalDbm(java.math.BigDecimal.valueOf(-18.7));
        serviceReq.setNotes("Instalação concluída");

        when(workOrderMapper.toCompleteInstallationRequest(any(TechnicianExecutionCompleteRequest.class))).thenReturn(serviceReq);
        when(fieldExecutionService.completeInstallation(eq(woId), eq(serviceReq))).thenReturn(wo);
        when(workOrderMapper.toResponse(wo)).thenReturn(woResponse);

        mockMvc.perform(post("/technician/execution/{workOrderId}/complete", woId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(woId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(fieldExecutionService).completeInstallation(eq(woId), eq(serviceReq));
    }
}
