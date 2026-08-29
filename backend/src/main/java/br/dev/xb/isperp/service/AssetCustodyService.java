package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CheckoutToolRequest;
import br.dev.xb.isperp.dto.CreateTransferRequest;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetCustodyService {

    private final SerializedAssetRepository assetRepository;
    private final StockTransferRepository transferRepository;
    private final StockTransferItemRepository transferItemRepository;
    private final ToolCustodyAgreementRepository agreementRepository;
    private final CustodyLogRepository custodyLogRepository;

    public List<SerializedAsset> getAllAssets() {
        return assetRepository.findAll();
    }

    public List<SerializedAsset> getAssetsByWarehouse(@NonNull UUID warehouseId) {
        return assetRepository.findByCurrentWarehouseIdAndStatus(warehouseId, SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO);
    }

    public List<SerializedAsset> getAssetsByHolder(@NonNull UUID holderUserId) {
        return assetRepository.findByCurrentHolderUserIdAndStatus(holderUserId, SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR);
    }

    public List<StockTransfer> getAllTransfers() {
        return transferRepository.findAll();
    }

    public List<ToolCustodyAgreement> getAllToolAgreements() {
        return agreementRepository.findAll();
    }

    @Transactional
    public StockTransfer createTransfer(@NonNull CreateTransferRequest request) {
        String code = "TRF-" + System.currentTimeMillis() % 1000000;
        log.info("Criando guia de transferência {}: de {} para {}", code, request.getOriginWarehouseId(), request.getDestinationWarehouseId());

        StockTransfer transfer = StockTransfer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code(code)
                .originWarehouseId(request.getOriginWarehouseId())
                .destinationWarehouseId(request.getDestinationWarehouseId())
                .carrierUserId(request.getCarrierUserId())
                .carrierName(request.getCarrierName())
                .carrierDocument(request.getCarrierDocument())
                .carrierType(request.getCarrierType() != null ? request.getCarrierType() : StockTransfer.CarrierType.COLABORADOR)
                .status(StockTransfer.TransferStatus.PENDING)
                .notes(request.getNotes())
                .build();

        StockTransfer savedTransfer = transferRepository.save(transfer);

        if (request.getAssetIds() != null) {
            for (UUID assetId : request.getAssetIds()) {
                StockTransferItem item = StockTransferItem.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .transferId(savedTransfer.getId())
                        .assetId(assetId)
                        .quantity(1)
                        .build();
                transferItemRepository.save(item);
            }
        }

        return savedTransfer;
    }

    @Transactional
    public StockTransfer dispatchTransfer(@NonNull UUID transferId, UUID dispatchedByUserId, String dispatchPhotoUrl) {
        StockTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transferência não encontrada"));

        transfer.setStatus(StockTransfer.TransferStatus.IN_TRANSIT);
        transfer.setDispatchedByUserId(dispatchedByUserId);
        transfer.setDispatchPhotoUrl(dispatchPhotoUrl);
        transfer.setDispatchedAt(LocalDateTime.now());

        StockTransfer saved = transferRepository.save(transfer);

        // Atualiza status dos equipamentos para EM_TRANSITO e prende à custódia do portador
        List<StockTransferItem> items = transferItemRepository.findByTransferId(transferId);
        for (StockTransferItem item : items) {
            if (item.getAssetId() != null) {
                assetRepository.findById(item.getAssetId()).ifPresent(asset -> {
                    asset.setStatus(SerializedAsset.AssetStatus.EM_TRANSITO);
                    asset.setCurrentHolderUserId(transfer.getCarrierUserId());
                    asset.setCurrentWarehouseId(null);
                    asset.setLastMovementAt(LocalDateTime.now());
                    assetRepository.save(asset);

                    // Grava log imutável de custódia
                    CustodyLog logEntry = CustodyLog.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .assetId(asset.getId())
                            .fromWarehouseId(transfer.getOriginWarehouseId())
                            .toUserId(transfer.getCarrierUserId())
                            .eventType("TRANSFER_DISPATCH")
                            .photoUrl(dispatchPhotoUrl)
                            .notes("Despachado em transferência " + transfer.getCode() + " com portador " + transfer.getCarrierName())
                            .build();
                    custodyLogRepository.save(logEntry);
                });
            }
        }

        log.info("Transferência {} despachada com sucesso. Itens sob custódia de {}", transfer.getCode(), transfer.getCarrierName());
        return saved;
    }

    @Transactional
    public StockTransfer confirmReceiptTransfer(@NonNull UUID transferId, UUID receivedByUserId, String receiptPhotoUrl) {
        StockTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new RuntimeException("Transferência não encontrada"));

        transfer.setStatus(StockTransfer.TransferStatus.RECEIVED);
        transfer.setReceivedByUserId(receivedByUserId);
        transfer.setReceiptPhotoUrl(receiptPhotoUrl);
        transfer.setReceivedAt(LocalDateTime.now());

        StockTransfer saved = transferRepository.save(transfer);

        // Entra no estoque de destino e libera o portador
        List<StockTransferItem> items = transferItemRepository.findByTransferId(transferId);
        for (StockTransferItem item : items) {
            if (item.getAssetId() != null) {
                assetRepository.findById(item.getAssetId()).ifPresent(asset -> {
                    asset.setStatus(SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO);
                    asset.setCurrentWarehouseId(transfer.getDestinationWarehouseId());
                    asset.setCurrentHolderUserId(null); // Desvincula da pessoa do portador
                    asset.setLastMovementAt(LocalDateTime.now());
                    assetRepository.save(asset);

                    // Grava log imutável de recebimento
                    CustodyLog logEntry = CustodyLog.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .assetId(asset.getId())
                            .fromUserId(transfer.getCarrierUserId())
                            .toWarehouseId(transfer.getDestinationWarehouseId())
                            .eventType("TRANSFER_RECEIPT")
                            .photoUrl(receiptPhotoUrl)
                            .notes("Recebido e conferido no destino " + transfer.getCode())
                            .build();
                    custodyLogRepository.save(logEntry);
                });
            }
        }

        log.info("Transferência {} recebida e conferida com sucesso no destino", transfer.getCode());
        return saved;
    }

    @Transactional
    public ToolCustodyAgreement checkoutToolAgreement(@NonNull CheckoutToolRequest request) {
        String code = "NOT-PROM-" + System.currentTimeMillis() % 1000000;

        BigDecimal totalValue = BigDecimal.ZERO;
        StringBuilder toolsDescription = new StringBuilder();

        for (UUID assetId : request.getAssetIds()) {
            SerializedAsset asset = assetRepository.findById(assetId)
                    .orElseThrow(() -> new RuntimeException("Equipamento não encontrado: " + assetId));
            BigDecimal val = asset.getReplacementValue() != null ? asset.getReplacementValue() : BigDecimal.ZERO;
            totalValue = totalValue.add(val);

            toolsDescription.append("- ").append(asset.getBrandModel())
                    .append(" (Serial: ").append(asset.getSerialNumber()).append(")")
                    .append(" - Valor: R$ ").append(val).append("\n");

            // Atualiza status do ativo para custódia do colaborador
            asset.setStatus(SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR);
            asset.setCurrentHolderUserId(request.getHolderUserId());
            asset.setCurrentWarehouseId(null);
            asset.setLastMovementAt(LocalDateTime.now());
            assetRepository.save(asset);

            CustodyLog logEntry = CustodyLog.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .assetId(asset.getId())
                    .toUserId(request.getHolderUserId())
                    .workOrderId(request.getWorkOrderId())
                    .eventType("CHECKOUT_TO_TECH")
                    .photoUrl(request.getDispatchPhotoUrl())
                    .notes("Cautela de ferramenta com força executiva: " + code)
                    .build();
            custodyLogRepository.save(logEntry);
        }

        if (request.getTotalPromissoryValue() != null && request.getTotalPromissoryValue().compareTo(BigDecimal.ZERO) > 0) {
            totalValue = request.getTotalPromissoryValue();
        }

        String agreementText = String.format(
                "TERMO DE CAUTELA, COMODATO E NOTA PROMISSÓRIA EXECUTIVA Nº %s\n\n" +
                "Eu, %s, portador(a) do CPF nº %s, declaro que retirei sob minha exclusiva responsabilidade e guarda os seguintes equipamentos de alto valor patrimonial da empresa:\n\n%s\n" +
                "VALOR TOTAL DE REPOSIÇÃO (LÍQUIDO, CERTO E EXIGÍVEL): R$ %.2f.\n\n" +
                "Comprometo-me a zelar pela integridade física e funcional dos bens e a devolvê-los incontinenti ao término do serviço. " +
                "Declaro ciência de que a não devolução ou avaria injustificada ensejará cobrança e execução judicial imediata deste título executivo nos termos da legislação vigente.\n\n" +
                "Data: %s",
                code, request.getHolderName(), request.getHolderCpf(), toolsDescription.toString(),
                totalValue, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );

        ToolCustodyAgreement agreement = ToolCustodyAgreement.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .code(code)
                .workOrderId(request.getWorkOrderId())
                .holderUserId(request.getHolderUserId())
                .holderName(request.getHolderName())
                .holderCpf(request.getHolderCpf())
                .isThirdParty(request.getIsThirdParty() != null ? request.getIsThirdParty() : false)
                .totalPromissoryValue(totalValue)
                .status(ToolCustodyAgreement.AgreementStatus.ACTIVE)
                .agreementText(agreementText)
                .dispatchPhotoUrl(request.getDispatchPhotoUrl())
                .notes(request.getNotes())
                .signedAt(LocalDateTime.now())
                .build();

        log.info("Termo de cautela executiva {} emitido no valor de R$ {} para {}", code, totalValue, request.getHolderName());
        return agreementRepository.save(agreement);
    }

    @Transactional
    public ToolCustodyAgreement returnToolAgreement(@NonNull UUID agreementId, UUID warehouseId, boolean isDamaged, String returnPhotoUrl, String notes) {
        ToolCustodyAgreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Termo de cautela não encontrado"));

        agreement.setStatus(isDamaged ? ToolCustodyAgreement.AgreementStatus.RETURNED_DAMAGED : ToolCustodyAgreement.AgreementStatus.RETURNED_OK);
        agreement.setReturnPhotoUrl(returnPhotoUrl);
        agreement.setReturnedAt(LocalDateTime.now());
        if (notes != null) {
            agreement.setNotes((agreement.getNotes() != null ? agreement.getNotes() + "\n" : "") + notes);
        }

        // Devolve ativos vinculados ao responsável de volta ao depósito
        if (agreement.getHolderUserId() != null) {
            List<SerializedAsset> heldAssets = assetRepository.findByCurrentHolderUserIdAndStatus(
                    agreement.getHolderUserId(), SerializedAsset.AssetStatus.CUSTODIA_COLABORADOR
            );
            for (SerializedAsset asset : heldAssets) {
                asset.setStatus(isDamaged ? SerializedAsset.AssetStatus.DEFEITO_TRIAGEM : SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO);
                asset.setCurrentWarehouseId(warehouseId);
                asset.setCurrentHolderUserId(null); // Libera o CPF do portador
                asset.setLastMovementAt(LocalDateTime.now());
                assetRepository.save(asset);

                CustodyLog logEntry = CustodyLog.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .assetId(asset.getId())
                        .fromUserId(agreement.getHolderUserId())
                        .toWarehouseId(warehouseId)
                        .eventType("RETURN_FROM_TECH")
                        .photoUrl(returnPhotoUrl)
                        .notes("Devolução do termo " + agreement.getCode() + (isDamaged ? " [AVARIADO/TRIAGEM]" : " [INTEGRO/BOM]"))
                        .build();
                custodyLogRepository.save(logEntry);
            }
        }

        log.info("Termo de cautela {} finalizado com status {}", agreement.getCode(), agreement.getStatus());
        return agreementRepository.save(agreement);
    }

    @Transactional
    public SerializedAsset returnAssetFromWorkOrder(@NonNull UUID assetId, @NonNull UUID warehouseId, boolean isDamaged, String photoUrl, String notes) {
        SerializedAsset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Ativo não encontrado"));

        UUID previousHolder = asset.getCurrentHolderUserId();
        asset.setStatus(isDamaged ? SerializedAsset.AssetStatus.DEFEITO_TRIAGEM : SerializedAsset.AssetStatus.DISPONIVEL_DEPOSITO);
        asset.setCurrentWarehouseId(warehouseId);
        asset.setCurrentHolderUserId(null);
        asset.setCurrentCustomerId(null);
        asset.setCurrentContractId(null);
        asset.setLastMovementAt(LocalDateTime.now());

        SerializedAsset saved = assetRepository.save(asset);

        CustodyLog logEntry = CustodyLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .assetId(saved.getId())
                .fromUserId(previousHolder)
                .toWarehouseId(warehouseId)
                .eventType("RECOVERED_FROM_CLIENT")
                .photoUrl(photoUrl)
                .notes("Logística reversa / Devolução de O.S.: " + notes)
                .build();
        custodyLogRepository.save(logEntry);

        log.info("Equipamento {} recebido no depósito {} via logística reversa", asset.getSerialNumber(), warehouseId);
        return saved;
    }
}
