package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CheckoutToolRequest;
import br.dev.xb.isperp.dto.CreateTransferRequest;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AssetCustodyServiceTest {

    @Mock
    private SerializedAssetRepository assetRepository;

    @Mock
    private StockTransferRepository transferRepository;

    @Mock
    private StockTransferItemRepository transferItemRepository;

    @Mock
    private ToolCustodyAgreementRepository agreementRepository;

    @Mock
    private CustodyLogRepository custodyLogRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @InjectMocks
    private AssetCustodyService assetCustodyService;

    private UUID originWarehouseId;
    private UUID destWarehouseId;
    private UUID carrierUserId;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        originWarehouseId = UuidCreatorUtils.generateUuidV7();
        destWarehouseId = UuidCreatorUtils.generateUuidV7();
        carrierUserId = UuidCreatorUtils.generateUuidV7();
        assetId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve criar transferência intermunicipal com status PENDING")
    void shouldCreateInterWarehouseTransfer() {
        CreateTransferRequest request = CreateTransferRequest.builder()
                .originWarehouseId(originWarehouseId)
                .destinationWarehouseId(destWarehouseId)
                .carrierUserId(carrierUserId)
                .carrierName("João Técnico")
                .carrierDocument("123.456.789-00")
                .carrierType(StockTransfer.CarrierType.COLABORADOR)
                .assetIds(List.of(assetId))
                .notes("Transferência para atendimento em Vitória do Xingu")
                .build();

        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(transferItemRepository.save(any(StockTransferItem.class))).thenAnswer(i -> i.getArgument(0));

        StockTransfer transfer = assetCustodyService.createTransfer(request);

        assertNotNull(transfer);
        assertEquals(StockTransfer.TransferStatus.PENDING, transfer.getStatus());
        assertEquals("João Técnico", transfer.getCarrierName());
        verify(transferRepository, times(1)).save(any(StockTransfer.class));
        verify(transferItemRepository, times(1)).save(any(StockTransferItem.class));
    }

    @Test
    @DisplayName("Deve despachar transferência vinculando itens à custódia do portador")
    void shouldDispatchTransferAndAssignCustodyToCarrier() {
        UUID transferId = UuidCreatorUtils.generateUuidV7();
        StockTransfer transfer = StockTransfer.builder()
                .id(transferId)
                .code("TRF-123456")
                .originWarehouseId(originWarehouseId)
                .destinationWarehouseId(destWarehouseId)
                .carrierUserId(carrierUserId)
                .carrierName("João Técnico")
                .carrierDocument("123.456.789-00")
                .status(StockTransfer.TransferStatus.PENDING)
                .build();

        SerializedAsset asset = SerializedAsset.builder()
                .id(assetId)
                .serialNumber("HWTC001122")
                .brandModel("ONT Wi-Fi 6")
                .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                .currentWarehouseId(originWarehouseId)
                .build();

        StockTransferItem item = StockTransferItem.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .transferId(transferId)
                .assetId(assetId)
                .build();

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(transferItemRepository.findByTransferId(transferId)).thenReturn(List.of(item));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(SerializedAsset.class))).thenAnswer(i -> i.getArgument(0));

        StockTransfer dispatched = assetCustodyService.dispatchTransfer(transferId, carrierUserId, "http://foto-despacho.jpg");

        assertEquals(StockTransfer.TransferStatus.IN_TRANSIT, dispatched.getStatus());
        assertEquals(SerializedAsset.AssetStatus.EM_TRANSITO, asset.getStatus());
        assertEquals(carrierUserId, asset.getCurrentHolderUserId());
        assertNull(asset.getCurrentWarehouseId());
        verify(custodyLogRepository, times(1)).save(any(CustodyLog.class));
    }

    @Test
    @DisplayName("Deve gerar Termo de Cautela e Nota Promissória para ferramentas de alto valor")
    void shouldCheckoutToolWithPromissoryNoteAgreement() {
        SerializedAsset fusionMachine = SerializedAsset.builder()
                .id(assetId)
                .serialNumber("FSM-90S-001")
                .brandModel("Máquina de Fusão Fujikura 90S+")
                .category(SerializedAsset.AssetCategory.TOOL_FUSION_MACHINE)
                .replacementValue(new BigDecimal("18500.00"))
                .status(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO)
                .build();

        CheckoutToolRequest request = CheckoutToolRequest.builder()
                .holderUserId(carrierUserId)
                .holderName("Pedro Prestador Terceiro")
                .holderCpf("999.888.777-66")
                .isThirdParty(true)
                .assetIds(List.of(assetId))
                .notes("Empréstimo para fusão do anel óptico de Vitória do Xingu")
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(fusionMachine));
        when(assetRepository.save(any(SerializedAsset.class))).thenAnswer(i -> i.getArgument(0));
        when(agreementRepository.save(any(ToolCustodyAgreement.class))).thenAnswer(i -> i.getArgument(0));

        ToolCustodyAgreement agreement = assetCustodyService.checkoutToolAgreement(request);

        assertNotNull(agreement);
        assertEquals(new BigDecimal("18500.00"), agreement.getTotalPromissoryValue());
        assertTrue(agreement.getAgreementText().contains("NOTA PROMISSÓRIA EXECUTIVA"));
        assertTrue(agreement.getAgreementText().contains("Pedro Prestador Terceiro"));
        assertTrue(agreement.getAgreementText().contains("18500.00"));
        assertEquals(SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR, fusionMachine.getStatus());
        assertEquals(carrierUserId, fusionMachine.getCurrentHolderUserId());
    }

    @Test
    @DisplayName("Regra de Ouro: Deve rejeitar retirada de material caso O.S. não seja informada")
    void shouldRejectMaterialCheckoutWithoutWorkOrder() {
        var request = br.dev.xb.isperp.dto.MaterialCheckoutOsRequest.builder()
                .workOrderId(null)
                .technicianUserId(carrierUserId)
                .quantityOrMeters(100)
                .build();

        assertThrows(IllegalArgumentException.class, () -> assetCustodyService.checkoutMaterialForWorkOrder(request));
    }

    @Test
    @DisplayName("Deve autorizar saída de material vinculado à O.S. e ao CPF do técnico")
    void shouldAuthorizeMaterialCheckoutWithWorkOrder() {
        UUID woId = UuidCreatorUtils.generateUuidV7();
        WorkOrder wo = WorkOrder.builder().id(woId).build();

        var request = br.dev.xb.isperp.dto.MaterialCheckoutOsRequest.builder()
                .workOrderId(woId)
                .technicianUserId(carrierUserId)
                .warehouseId(originWarehouseId)
                .itemCode("DROP-OPT-1FO")
                .quantityOrMeters(200)
                .beforePhotoUrl("http://foto-metro-inicial.jpg")
                .notes("Retirada de bobina para O.S.")
                .build();

        when(workOrderRepository.findById(woId)).thenReturn(Optional.of(wo));
        when(custodyLogRepository.save(any(CustodyLog.class))).thenAnswer(i -> i.getArgument(0));

        CustodyLog log = assetCustodyService.checkoutMaterialForWorkOrder(request);

        assertNotNull(log);
        assertEquals("MATERIAL_CHECKOUT_OS", log.getEventType());
        assertEquals(woId, log.getWorkOrderId());
        assertEquals(carrierUserId, log.getToUserId());
        assertEquals("http://foto-metro-inicial.jpg", log.getPhotoUrl());
    }

    @Test
    @DisplayName("Deve registrar devolução com ressalva de divergência e emitir evento caso metragem não coincida")
    void shouldRegisterCheckinWithDivergenceWhenMetersDoNotMatch() {
        UUID woId = UuidCreatorUtils.generateUuidV7();

        // Inicial: 2.000m, Consumo apontado na O.S.: 500m -> Esperava: 1.500m. Apurado restante: 1.200m (faltando 300m)
        var request = br.dev.xb.isperp.dto.MaterialCheckinOsRequest.builder()
                .workOrderId(woId)
                .technicianUserId(carrierUserId)
                .warehouseId(originWarehouseId)
                .itemCode("DROP-OPT-1FO")
                .initialMetersOrQty(2000)
                .consumedMetersOrQty(500)
                .actualRemainingMetersOrQty(1200)
                .beforePhotoUrl("http://foto-inicial.jpg")
                .installedPhotoUrl("http://foto-poste.jpg")
                .returnPhotoUrl("http://foto-final.jpg")
                .notes("Divergência de 300m a justificar")
                .build();

        when(custodyLogRepository.save(any(CustodyLog.class))).thenAnswer(i -> i.getArgument(0));

        var response = assetCustodyService.checkinMaterialForWorkOrder(request);

        assertNotNull(response);
        assertTrue(response.isHasDivergence());
        assertEquals("DIVERGENT", response.getStatus());
        assertEquals(1500, response.getExpectedRemaining());
        assertEquals(1200, response.getActualRemaining());
        assertEquals(-300, response.getDivergenceQuantity());
        verify(domainEventPublisher, times(1)).publish(any());
        verify(custodyLogRepository, times(1)).save(any(CustodyLog.class));
    }
}
