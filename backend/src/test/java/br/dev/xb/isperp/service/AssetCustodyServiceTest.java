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
}
