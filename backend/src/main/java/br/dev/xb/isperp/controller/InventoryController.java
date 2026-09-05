package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.InventoryApi;
import br.dev.xb.isperp.api.dto.InventoryItemResponse;
import br.dev.xb.isperp.mapper.InventoryMapper;
import br.dev.xb.isperp.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class InventoryController implements InventoryApi {

    private final InventoryService inventoryService;
    private final InventoryMapper inventoryMapper;

    @Override
    public ResponseEntity<List<InventoryItemResponse>> getAllInventoryItems() {
        return ResponseEntity.ok(inventoryMapper.toInventoryItemResponseList(inventoryService.getAllItems()));
    }

    @org.springframework.web.bind.annotation.PostMapping({"/inventory/entry", "/api/inventory/entry"})
    public ResponseEntity<InventoryItemResponse> registerStockEntry(
            @org.springframework.web.bind.annotation.RequestBody br.dev.xb.isperp.dto.StockEntryRequest request) {
        var item = inventoryService.registerStockEntry(
                request.getWarehouseId(),
                request.getItemCode(),
                request.getItemName(),
                request.getCategory(),
                request.getQuantity(),
                request.getUnit(),
                request.getNotes()
        );
        return ResponseEntity.ok(inventoryMapper.toResponse(item));
    }
}
