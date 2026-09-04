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
}
