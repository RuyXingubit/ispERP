package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.AssetCustodyApi;
import br.dev.xb.isperp.api.dto.*;
import br.dev.xb.isperp.entity.SerializedAsset;
import br.dev.xb.isperp.entity.StockTransfer;
import br.dev.xb.isperp.entity.ToolCustodyAgreement;
import br.dev.xb.isperp.mapper.InventoryMapper;
import br.dev.xb.isperp.service.AssetCustodyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class AssetCustodyController implements AssetCustodyApi {

    private final AssetCustodyService assetCustodyService;
    private final InventoryMapper inventoryMapper;

    @Override
    public ResponseEntity<List<SerializedAssetResponse>> getAllAssets() {
        return ResponseEntity.ok(inventoryMapper.toSerializedAssetResponseList(assetCustodyService.getAllAssets()));
    }

    @Override
    public ResponseEntity<List<SerializedAssetResponse>> getAssetsByWarehouse(UUID warehouseId) {
        return ResponseEntity.ok(inventoryMapper.toSerializedAssetResponseList(assetCustodyService.getAssetsByWarehouse(warehouseId)));
    }

    @Override
    public ResponseEntity<List<SerializedAssetResponse>> getAssetsByHolder(UUID holderUserId) {
        return ResponseEntity.ok(inventoryMapper.toSerializedAssetResponseList(assetCustodyService.getAssetsByHolder(holderUserId)));
    }

    @Override
    public ResponseEntity<List<StockTransferResponse>> getAllTransfers() {
        return ResponseEntity.ok(inventoryMapper.toStockTransferResponseList(assetCustodyService.getAllTransfers()));
    }

    @Override
    public ResponseEntity<StockTransferResponse> createTransfer(CreateTransferRequest createTransferRequest) {
        br.dev.xb.isperp.dto.CreateTransferRequest serviceRequest = inventoryMapper.toServiceRequest(createTransferRequest);
        StockTransfer saved = assetCustodyService.createTransfer(serviceRequest);
        return ResponseEntity.ok(inventoryMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<StockTransferResponse> dispatchTransfer(UUID id, DispatchTransferRequest dispatchTransferRequest) {
        UUID userId = dispatchTransferRequest != null ? dispatchTransferRequest.getUserId() : null;
        String photoUrl = dispatchTransferRequest != null ? dispatchTransferRequest.getDispatchPhotoUrl() : null;
        StockTransfer saved = assetCustodyService.dispatchTransfer(id, userId, photoUrl);
        return ResponseEntity.ok(inventoryMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<StockTransferResponse> confirmReceiptTransfer(UUID id, ConfirmReceiptTransferRequest confirmReceiptTransferRequest) {
        UUID userId = confirmReceiptTransferRequest != null ? confirmReceiptTransferRequest.getUserId() : null;
        String photoUrl = confirmReceiptTransferRequest != null ? confirmReceiptTransferRequest.getReceiptPhotoUrl() : null;
        StockTransfer saved = assetCustodyService.confirmReceiptTransfer(id, userId, photoUrl);
        return ResponseEntity.ok(inventoryMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<List<ToolCustodyAgreementResponse>> getAllToolAgreements() {
        return ResponseEntity.ok(inventoryMapper.toToolCustodyAgreementResponseList(assetCustodyService.getAllToolAgreements()));
    }

    @Override
    public ResponseEntity<ToolCustodyAgreementResponse> checkoutToolAgreement(CheckoutToolRequest checkoutToolRequest) {
        br.dev.xb.isperp.dto.CheckoutToolRequest serviceRequest = inventoryMapper.toServiceRequest(checkoutToolRequest);
        ToolCustodyAgreement saved = assetCustodyService.checkoutToolAgreement(serviceRequest);
        return ResponseEntity.ok(inventoryMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<ToolCustodyAgreementResponse> returnToolAgreement(UUID id, ReturnToolAgreementRequest returnToolAgreementRequest) {
        UUID warehouseId = returnToolAgreementRequest != null ? returnToolAgreementRequest.getWarehouseId() : null;
        boolean isDamaged = returnToolAgreementRequest != null && Boolean.TRUE.equals(returnToolAgreementRequest.getIsDamaged());
        String photoUrl = returnToolAgreementRequest != null ? returnToolAgreementRequest.getReturnPhotoUrl() : null;
        String notes = returnToolAgreementRequest != null ? returnToolAgreementRequest.getNotes() : null;

        ToolCustodyAgreement saved = assetCustodyService.returnToolAgreement(id, warehouseId, isDamaged, photoUrl, notes);
        return ResponseEntity.ok(inventoryMapper.toResponse(saved));
    }

    @Override
    public ResponseEntity<SerializedAssetResponse> returnAssetFromWorkOrder(UUID id, ReverseLogisticsRequest reverseLogisticsRequest) {
        UUID warehouseId = reverseLogisticsRequest.getWarehouseId();
        boolean isDamaged = Boolean.TRUE.equals(reverseLogisticsRequest.getIsDamaged());
        String photoUrl = reverseLogisticsRequest.getPhotoUrl();
        String notes = reverseLogisticsRequest.getNotes();

        SerializedAsset saved = assetCustodyService.returnAssetFromWorkOrder(id, warehouseId, isDamaged, photoUrl, notes);
        return ResponseEntity.ok(inventoryMapper.toResponse(saved));
    }

    @org.springframework.web.bind.annotation.PostMapping({"/inventory/custody/materials/checkout-os", "/api/inventory/custody/materials/checkout-os"})
    public ResponseEntity<br.dev.xb.isperp.entity.CustodyLog> checkoutMaterialForWorkOrder(
            @org.springframework.web.bind.annotation.RequestBody br.dev.xb.isperp.dto.MaterialCheckoutOsRequest request) {
        return ResponseEntity.ok(assetCustodyService.checkoutMaterialForWorkOrder(request));
    }

    @org.springframework.web.bind.annotation.PostMapping({"/inventory/custody/materials/checkin-os", "/api/inventory/custody/materials/checkin-os"})
    public ResponseEntity<br.dev.xb.isperp.dto.MaterialCheckinResponse> checkinMaterialForWorkOrder(
            @org.springframework.web.bind.annotation.RequestBody br.dev.xb.isperp.dto.MaterialCheckinOsRequest request) {
        return ResponseEntity.ok(assetCustodyService.checkinMaterialForWorkOrder(request));
    }
}
