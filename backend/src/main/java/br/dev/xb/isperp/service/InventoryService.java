package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.InventoryItem;
import br.dev.xb.isperp.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.jspecify.annotations.Nullable;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public List<InventoryItem> getAllItems() {
        return inventoryItemRepository.findAll();
    }

    public Optional<InventoryItem> getItemByCode(String code) {
        return inventoryItemRepository.findByCode(code);
    }

    public InventoryItem saveItem(InventoryItem item) {
        return inventoryItemRepository.save(item);
    }

    @Transactional
    public InventoryItem registerStockEntry(UUID warehouseId, String itemCode, String itemName, String category, int quantity, String unit, @Nullable String notes) {
        InventoryItem item = inventoryItemRepository.findByCode(itemCode)
                .orElseGet(() -> {
                    InventoryItem newItem = InventoryItem.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .code(itemCode)
                            .name(itemName != null && !itemName.isBlank() ? itemName : itemCode)
                            .category(category != null && !category.isBlank() ? category : "GERAL")
                            .quantityInStock(0)
                            .minQuantity(10)
                            .unit(unit != null && !unit.isBlank() ? unit : "UN")
                            .build();
                    return inventoryItemRepository.save(newItem);
                });

        item.setQuantityInStock(item.getQuantityInStock() + quantity);
        if (itemName != null && !itemName.isBlank()) {
            item.setName(itemName);
        }
        if (unit != null && !unit.isBlank()) {
            item.setUnit(unit);
        }
        InventoryItem saved = inventoryItemRepository.save(item);

        log.info("Entrada de estoque registrada: {} (+{} {}), saldo atual={}", itemCode, quantity, item.getUnit(), saved.getQuantityInStock());
        return saved;
    }

    /**
     * Valida e reserva materiais essenciais para uma nova instalação.
     *
     * @param contractId Identificador do contrato
     * @return Lista de alertas ou mensagens sobre o estoque
     */
    @Transactional
    public List<String> checkAndReserveInstallationMaterials(UUID contractId) {
        log.info("Verificando insumos de estoque para instalação do contrato: {}", contractId);
        List<String> warnings = new ArrayList<>();

        // 1. Verificar ONT / ONU
        inventoryItemRepository.findByCode("ONT-WIFI6-XPON").ifPresentOrElse(item -> {
            if (item.getQuantityInStock() < 1) {
                warnings.add("ALERTA: Estoque zerado de ONTs (XPON Wi-Fi 6)");
            } else {
                item.setQuantityInStock(item.getQuantityInStock() - 1);
                inventoryItemRepository.save(item);
                log.info("1x ONT reservada. Saldo restante: {}", item.getQuantityInStock());
            }
        }, () -> warnings.add("Item ONT-WIFI6-XPON não cadastrado no estoque"));

        // 2. Verificar Cabo Drop (reserva padrão estimada de 100m)
        inventoryItemRepository.findByCode("DROP-OPT-1FO").ifPresentOrElse(item -> {
            if (item.getQuantityInStock() < 100) {
                warnings.add("ALERTA: Cabo drop em nível crítico (" + item.getQuantityInStock() + "m)");
            } else {
                item.setQuantityInStock(item.getQuantityInStock() - 100);
                inventoryItemRepository.save(item);
            }
        }, () -> warnings.add("Item DROP-OPT-1FO não cadastrado no estoque"));

        // 3. Verificar Conectores SC/APC (2 unidades por instalação)
        inventoryItemRepository.findByCode("CON-SCAPC-FAST").ifPresentOrElse(item -> {
            if (item.getQuantityInStock() < 2) {
                warnings.add("ALERTA: Conectores SC/APC insuficientes");
            } else {
                item.setQuantityInStock(item.getQuantityInStock() - 2);
                inventoryItemRepository.save(item);
            }
        }, () -> warnings.add("Item CON-SCAPC-FAST não cadastrado no estoque"));

        return warnings;
    }
}
