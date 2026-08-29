package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.InventoryItem;
import br.dev.xb.isperp.repository.InventoryItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;

    public List<InventoryItem> getAllItems() {
        return inventoryItemRepository.findAll();
    }

    public Optional<InventoryItem> getItemByCode(@NonNull String code) {
        return inventoryItemRepository.findByCode(code);
    }

    public InventoryItem saveItem(@NonNull InventoryItem item) {
        return inventoryItemRepository.save(item);
    }

    /**
     * Valida e reserva materiais essenciais para uma nova instalação.
     *
     * @param contractId Identificador do contrato
     * @return Lista de alertas ou mensagens sobre o estoque
     */
    @Transactional
    public List<String> checkAndReserveInstallationMaterials(@NonNull UUID contractId) {
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
