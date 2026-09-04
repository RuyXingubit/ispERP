package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.CheckoutToolRequest;
import br.dev.xb.isperp.dto.CreateTransferRequest;
import br.dev.xb.isperp.entity.SerializedAsset;
import br.dev.xb.isperp.entity.StockTransfer;
import br.dev.xb.isperp.entity.ToolCustodyAgreement;
import br.dev.xb.isperp.mapper.InventoryMapperImpl;
import br.dev.xb.isperp.service.AssetCustodyService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssetCustodyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(InventoryMapperImpl.class)
@SuppressWarnings("null")
class AssetCustodyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssetCustodyService assetCustodyService;

    @Test
    @DisplayName("GET /inventory/custody/assets - Deve listar todos os ativos serializados")
    void testGetAllAssets() throws Exception {
        UUID assetId = UuidCreatorUtils.generateUuidV7();
        SerializedAsset asset = SerializedAsset.builder()
                .id(assetId)
                .macAddress("A4:93:3F:11:22:33")
                .serialNumber("ZTEG98765432")
                .brandModel("ZTE F670L")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .replacementValue(new BigDecimal("250.00"))
                .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.getAllAssets()).thenReturn(List.of(asset));

        mockMvc.perform(get("/inventory/custody/assets")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()))
                .andExpect(jsonPath("$[0].serialNumber").value("ZTEG98765432"))
                .andExpect(jsonPath("$[0].category").value("ONU_ONT"))
                .andExpect(jsonPath("$[0].status").value("DISPONIVEL_DEPOSITO"))
                .andExpect(jsonPath("$[0].replacementValue").value(250.00));
    }

    @Test
    @DisplayName("GET /inventory/custody/assets/warehouse/{warehouseId} - Deve listar ativos por depósito")
    void testGetAssetsByWarehouse() throws Exception {
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();
        UUID assetId = UuidCreatorUtils.generateUuidV7();

        SerializedAsset asset = SerializedAsset.builder()
                .id(assetId)
                .serialNumber("HUAW12345678")
                .brandModel("Huawei EG8145V5")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .currentWarehouseId(warehouseId)
                .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.getAssetsByWarehouse(warehouseId)).thenReturn(List.of(asset));

        mockMvc.perform(get("/inventory/custody/assets/warehouse/{warehouseId}", warehouseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()))
                .andExpect(jsonPath("$[0].currentWarehouseId").value(warehouseId.toString()));
    }

    @Test
    @DisplayName("GET /inventory/custody/assets/holder/{holderUserId} - Deve listar ativos sob custódia do colaborador")
    void testGetAssetsByHolder() throws Exception {
        UUID holderUserId = UuidCreatorUtils.generateUuidV7();
        UUID assetId = UuidCreatorUtils.generateUuidV7();

        SerializedAsset asset = SerializedAsset.builder()
                .id(assetId)
                .serialNumber("FUS-998877")
                .brandModel("Fujikura 70S")
                .category(SerializedAsset.AssetCategory.TOOL_FUSION_MACHINE)
                .currentHolderUserId(holderUserId)
                .status(SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR)
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.getAssetsByHolder(holderUserId)).thenReturn(List.of(asset));

        mockMvc.perform(get("/inventory/custody/assets/holder/{holderUserId}", holderUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assetId.toString()))
                .andExpect(jsonPath("$[0].category").value("TOOL_FUSION_MACHINE"))
                .andExpect(jsonPath("$[0].currentHolderUserId").value(holderUserId.toString()));
    }

    @Test
    @DisplayName("POST /inventory/custody/transfers - Deve criar guia de transferência inter-bases")
    void testCreateTransfer() throws Exception {
        UUID transferId = UuidCreatorUtils.generateUuidV7();
        UUID originId = UuidCreatorUtils.generateUuidV7();
        UUID destId = UuidCreatorUtils.generateUuidV7();

        StockTransfer transfer = StockTransfer.builder()
                .id(transferId)
                .code("TRF-123456")
                .originWarehouseId(originId)
                .destinationWarehouseId(destId)
                .carrierName("Marcos Transportador")
                .carrierDocument("111.222.333-44")
                .carrierType(StockTransfer.CarrierType.COLABORADOR)
                .status(StockTransfer.TransferStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.createTransfer(any(CreateTransferRequest.class))).thenReturn(transfer);

        String jsonPayload = String.format("""
                {
                    "originWarehouseId": "%s",
                    "destinationWarehouseId": "%s",
                    "carrierName": "Marcos Transportador",
                    "carrierDocument": "111.222.333-44",
                    "carrierType": "COLABORADOR"
                }
                """, originId, destId);

        mockMvc.perform(post("/inventory/custody/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId.toString()))
                .andExpect(jsonPath("$.code").value("TRF-123456"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /inventory/custody/transfers/{id}/dispatch - Deve despachar transferência")
    void testDispatchTransfer() throws Exception {
        UUID transferId = UuidCreatorUtils.generateUuidV7();
        UUID userId = UuidCreatorUtils.generateUuidV7();

        StockTransfer transfer = StockTransfer.builder()
                .id(transferId)
                .code("TRF-123456")
                .originWarehouseId(UuidCreatorUtils.generateUuidV7())
                .destinationWarehouseId(UuidCreatorUtils.generateUuidV7())
                .carrierName("Marcos Transportador")
                .carrierDocument("111.222.333-44")
                .carrierType(StockTransfer.CarrierType.COLABORADOR)
                .status(StockTransfer.TransferStatus.IN_TRANSIT)
                .dispatchPhotoUrl("https://isperp.local/dispatch.jpg")
                .dispatchedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.dispatchTransfer(eq(transferId), eq(userId), anyString())).thenReturn(transfer);

        String jsonPayload = String.format("""
                {
                    "userId": "%s",
                    "dispatchPhotoUrl": "https://isperp.local/dispatch.jpg"
                }
                """, userId);

        mockMvc.perform(post("/inventory/custody/transfers/{id}/dispatch", transferId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId.toString()))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.dispatchPhotoUrl").value("https://isperp.local/dispatch.jpg"));
    }

    @Test
    @DisplayName("POST /inventory/custody/transfers/{id}/receive - Deve confirmar recebimento da transferência")
    void testConfirmReceiptTransfer() throws Exception {
        UUID transferId = UuidCreatorUtils.generateUuidV7();
        UUID userId = UuidCreatorUtils.generateUuidV7();

        StockTransfer transfer = StockTransfer.builder()
                .id(transferId)
                .code("TRF-123456")
                .originWarehouseId(UuidCreatorUtils.generateUuidV7())
                .destinationWarehouseId(UuidCreatorUtils.generateUuidV7())
                .carrierName("Marcos Transportador")
                .carrierDocument("111.222.333-44")
                .carrierType(StockTransfer.CarrierType.COLABORADOR)
                .status(StockTransfer.TransferStatus.RECEIVED)
                .receiptPhotoUrl("https://isperp.local/receipt.jpg")
                .receivedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.confirmReceiptTransfer(eq(transferId), eq(userId), anyString())).thenReturn(transfer);

        String jsonPayload = String.format("""
                {
                    "userId": "%s",
                    "receiptPhotoUrl": "https://isperp.local/receipt.jpg"
                }
                """, userId);

        mockMvc.perform(post("/inventory/custody/transfers/{id}/receive", transferId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transferId.toString()))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    @DisplayName("POST /inventory/custody/tool-agreements/checkout - Deve emitir termo de cautela de ferramental")
    void testCheckoutToolAgreement() throws Exception {
        UUID agreementId = UuidCreatorUtils.generateUuidV7();
        UUID assetId = UuidCreatorUtils.generateUuidV7();

        ToolCustodyAgreement agreement = ToolCustodyAgreement.builder()
                .id(agreementId)
                .code("NOT-PROM-001")
                .holderName("Pedro Técnico")
                .holderCpf("555.666.777-88")
                .isThirdParty(false)
                .totalPromissoryValue(new BigDecimal("8000.00"))
                .status(ToolCustodyAgreement.AgreementStatus.ACTIVE)
                .agreementText("Termo formal de cautela...")
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.checkoutToolAgreement(any(CheckoutToolRequest.class))).thenReturn(agreement);

        String jsonPayload = String.format("""
                {
                    "holderName": "Pedro Técnico",
                    "holderCpf": "555.666.777-88",
                    "isThirdParty": false,
                    "assetIds": ["%s"],
                    "totalPromissoryValue": 8000.00
                }
                """, assetId);

        mockMvc.perform(post("/inventory/custody/tool-agreements/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(agreementId.toString()))
                .andExpect(jsonPath("$.code").value("NOT-PROM-001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalPromissoryValue").value(8000.00));
    }

    @Test
    @DisplayName("POST /inventory/custody/tool-agreements/{id}/return - Deve dar baixa no termo de cautela")
    void testReturnToolAgreement() throws Exception {
        UUID agreementId = UuidCreatorUtils.generateUuidV7();
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();

        ToolCustodyAgreement agreement = ToolCustodyAgreement.builder()
                .id(agreementId)
                .code("NOT-PROM-001")
                .holderName("Pedro Técnico")
                .holderCpf("555.666.777-88")
                .status(ToolCustodyAgreement.AgreementStatus.RETURNED_OK)
                .returnedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.returnToolAgreement(eq(agreementId), eq(warehouseId), eq(false), any(), any()))
                .thenReturn(agreement);

        String jsonPayload = String.format("""
                {
                    "warehouseId": "%s",
                    "isDamaged": false,
                    "notes": "Devolvido em perfeitas condições"
                }
                """, warehouseId);

        mockMvc.perform(post("/inventory/custody/tool-agreements/{id}/return", agreementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(agreementId.toString()))
                .andExpect(jsonPath("$.status").value("RETURNED_OK"));
    }

    @Test
    @DisplayName("POST /inventory/custody/assets/{id}/reverse-logistics - Deve recolher ativo de cliente para triagem")
    void testReturnAssetFromWorkOrder() throws Exception {
        UUID assetId = UuidCreatorUtils.generateUuidV7();
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();

        SerializedAsset asset = SerializedAsset.builder()
                .id(assetId)
                .serialNumber("ONU-REC-001")
                .brandModel("ZTE F670L")
                .category(SerializedAsset.AssetCategory.ONU_ONT)
                .currentWarehouseId(warehouseId)
                .status(SerializedAsset.AssetStatus.DEFEITO_TRIAGEM)
                .createdAt(LocalDateTime.now())
                .build();

        when(assetCustodyService.returnAssetFromWorkOrder(eq(assetId), eq(warehouseId), eq(true), any(), any()))
                .thenReturn(asset);

        String jsonPayload = String.format("""
                {
                    "warehouseId": "%s",
                    "isDamaged": true,
                    "notes": "Porta PON com defeito após tempestade"
                }
                """, warehouseId);

        mockMvc.perform(post("/inventory/custody/assets/{id}/reverse-logistics", assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetId.toString()))
                .andExpect(jsonPath("$.status").value("DEFEITO_TRIAGEM"));
    }
}
