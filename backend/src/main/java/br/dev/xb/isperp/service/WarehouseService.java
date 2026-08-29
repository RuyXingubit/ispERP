package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Warehouse;
import br.dev.xb.isperp.repository.WarehouseRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    public List<Warehouse> getActiveWarehouses() {
        return warehouseRepository.findByActiveTrue();
    }

    public Optional<Warehouse> getWarehouseById(UUID id) {
        return warehouseRepository.findById(id);
    }

    @Transactional
    public Warehouse createWarehouse(Warehouse warehouse) {
        if (warehouse.getId() == null) {
            warehouse.setId(UuidCreatorUtils.generateUuidV7());
        }
        log.info("Cadastrando novo depósito/almoxarifado: {} ({})", warehouse.getName(), warehouse.getCode());
        return warehouseRepository.save(warehouse);
    }
}
