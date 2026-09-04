package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.WarehousesApi;
import br.dev.xb.isperp.api.dto.WarehouseCreateRequest;
import br.dev.xb.isperp.api.dto.WarehouseResponse;
import br.dev.xb.isperp.entity.Warehouse;
import br.dev.xb.isperp.mapper.InventoryMapper;
import br.dev.xb.isperp.service.WarehouseService;
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
public class WarehouseController implements WarehousesApi {

    private final WarehouseService warehouseService;
    private final InventoryMapper inventoryMapper;

    @Override
    public ResponseEntity<WarehouseResponse> createWarehouse(WarehouseCreateRequest warehouseCreateRequest) {
        Warehouse warehouse = inventoryMapper.toEntity(warehouseCreateRequest);
        Warehouse created = warehouseService.createWarehouse(warehouse);
        return ResponseEntity.ok(inventoryMapper.toResponse(created));
    }

    @Override
    public ResponseEntity<List<WarehouseResponse>> getActiveWarehouses() {
        return ResponseEntity.ok(inventoryMapper.toWarehouseResponseList(warehouseService.getActiveWarehouses()));
    }

    @Override
    public ResponseEntity<List<WarehouseResponse>> getAllWarehouses() {
        return ResponseEntity.ok(inventoryMapper.toWarehouseResponseList(warehouseService.getAllWarehouses()));
    }

    @Override
    public ResponseEntity<WarehouseResponse> getWarehouseById(UUID id) {
        return warehouseService.getWarehouseById(id)
                .map(w -> ResponseEntity.ok(inventoryMapper.toResponse(w)))
                .orElse(ResponseEntity.notFound().build());
    }
}
