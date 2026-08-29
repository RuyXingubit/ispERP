package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.CheckoutToolRequest;
import br.dev.xb.isperp.dto.CreateTransferRequest;
import br.dev.xb.isperp.entity.SerializedAsset;
import br.dev.xb.isperp.entity.StockTransfer;
import br.dev.xb.isperp.entity.ToolCustodyAgreement;
import br.dev.xb.isperp.service.AssetCustodyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory/custody")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class AssetCustodyController {

    private final AssetCustodyService assetCustodyService;

    @GetMapping("/assets")
    public ResponseEntity<List<SerializedAsset>> getAllAssets() {
        return ResponseEntity.ok(assetCustodyService.getAllAssets());
    }

    @GetMapping("/assets/warehouse/{warehouseId}")
    public ResponseEntity<List<SerializedAsset>> getAssetsByWarehouse(@PathVariable UUID warehouseId) {
        return ResponseEntity.ok(assetCustodyService.getAssetsByWarehouse(warehouseId));
    }

    @GetMapping("/assets/holder/{holderUserId}")
    public ResponseEntity<List<SerializedAsset>> getAssetsByHolder(@PathVariable UUID holderUserId) {
        return ResponseEntity.ok(assetCustodyService.getAssetsByHolder(holderUserId));
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<StockTransfer>> getAllTransfers() {
        return ResponseEntity.ok(assetCustodyService.getAllTransfers());
    }

    @PostMapping("/transfers")
    public ResponseEntity<StockTransfer> createTransfer(@Valid @RequestBody CreateTransferRequest request) {
        return ResponseEntity.ok(assetCustodyService.createTransfer(request));
    }

    @PostMapping("/transfers/{id}/dispatch")
    public ResponseEntity<StockTransfer> dispatchTransfer(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        UUID userId = body != null && body.containsKey("userId") ? UUID.fromString(body.get("userId")) : null;
        String photoUrl = body != null ? body.get("dispatchPhotoUrl") : null;
        return ResponseEntity.ok(assetCustodyService.dispatchTransfer(id, userId, photoUrl));
    }

    @PostMapping("/transfers/{id}/receive")
    public ResponseEntity<StockTransfer> confirmReceiptTransfer(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        UUID userId = body != null && body.containsKey("userId") ? UUID.fromString(body.get("userId")) : null;
        String photoUrl = body != null ? body.get("receiptPhotoUrl") : null;
        return ResponseEntity.ok(assetCustodyService.confirmReceiptTransfer(id, userId, photoUrl));
    }

    @GetMapping("/tool-agreements")
    public ResponseEntity<List<ToolCustodyAgreement>> getAllToolAgreements() {
        return ResponseEntity.ok(assetCustodyService.getAllToolAgreements());
    }

    @PostMapping("/tool-agreements/checkout")
    public ResponseEntity<ToolCustodyAgreement> checkoutToolAgreement(@Valid @RequestBody CheckoutToolRequest request) {
        return ResponseEntity.ok(assetCustodyService.checkoutToolAgreement(request));
    }

    @PostMapping("/tool-agreements/{id}/return")
    public ResponseEntity<ToolCustodyAgreement> returnToolAgreement(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID warehouseId = body.containsKey("warehouseId") ? UUID.fromString((String) body.get("warehouseId")) : null;
        boolean isDamaged = body.containsKey("isDamaged") && Boolean.parseBoolean(body.get("isDamaged").toString());
        String photoUrl = body.containsKey("returnPhotoUrl") ? (String) body.get("returnPhotoUrl") : null;
        String notes = body.containsKey("notes") ? (String) body.get("notes") : null;

        return ResponseEntity.ok(assetCustodyService.returnToolAgreement(id, warehouseId, isDamaged, photoUrl, notes));
    }

    @PostMapping("/assets/{id}/reverse-logistics")
    public ResponseEntity<SerializedAsset> returnAssetFromWorkOrder(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID warehouseId = UUID.fromString((String) body.get("warehouseId"));
        boolean isDamaged = body.containsKey("isDamaged") && Boolean.parseBoolean(body.get("isDamaged").toString());
        String photoUrl = body.containsKey("photoUrl") ? (String) body.get("photoUrl") : null;
        String notes = body.containsKey("notes") ? (String) body.get("notes") : null;

        return ResponseEntity.ok(assetCustodyService.returnAssetFromWorkOrder(id, warehouseId, isDamaged, photoUrl, notes));
    }
}
