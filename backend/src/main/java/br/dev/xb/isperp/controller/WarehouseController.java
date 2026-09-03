package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.Warehouse;
import br.dev.xb.isperp.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/warehouses", "/api/warehouses"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("null")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public ResponseEntity<List<Warehouse>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Warehouse>> getActiveWarehouses() {
        return ResponseEntity.ok(warehouseService.getActiveWarehouses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Warehouse> getWarehouseById(@PathVariable UUID id) {
        return warehouseService.getWarehouseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Warehouse> createWarehouse(@Valid @RequestBody Warehouse warehouse) {
        return ResponseEntity.ok(warehouseService.createWarehouse(warehouse));
    }
}
