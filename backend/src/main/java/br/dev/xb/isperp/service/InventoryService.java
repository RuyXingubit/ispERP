package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.StockMovementResponse;
import br.dev.xb.isperp.entity.CustodyLog;
import br.dev.xb.isperp.entity.InventoryItem;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.Warehouse;
import br.dev.xb.isperp.repository.CustodyLogRepository;
import br.dev.xb.isperp.repository.InventoryItemRepository;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.WarehouseRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final CustodyLogRepository custodyLogRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    public List<InventoryItem> getAllItems() {
        return inventoryItemRepository.findAll();
    }

    public Optional<InventoryItem> getItemByCode(String code) {
        return inventoryItemRepository.findByCode(code);
    }

    public Optional<InventoryItem> getItemById(UUID id) {
        return inventoryItemRepository.findById(id);
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

        int newBalance = item.getQuantityInStock() + quantity;
        item.setQuantityInStock(newBalance);
        if (itemName != null && !itemName.isBlank()) {
            item.setName(itemName);
        }
        if (unit != null && !unit.isBlank()) {
            item.setUnit(unit);
        }
        InventoryItem saved = inventoryItemRepository.save(item);

        CustodyLog logEntry = CustodyLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .itemId(saved.getId())
                .toWarehouseId(warehouseId)
                .eventType("STOCK_ENTRY")
                .quantity(quantity)
                .balanceAfter(newBalance)
                .notes(notes != null && !notes.isBlank() ? notes : "Entrada / Lote de fornecedor: +" + quantity + " " + saved.getUnit())
                .build();
        custodyLogRepository.save(logEntry);

        log.info("Entrada de estoque registrada: {} (+{} {}), saldo atual={}", itemCode, quantity, item.getUnit(), saved.getQuantityInStock());
        return saved;
    }

    /**
     * Obtém o extrato cronológico de movimentações e rastreabilidade (Kardex) de um insumo.
     */
    public List<StockMovementResponse> getItemMovements(UUID itemId) {
        List<CustodyLog> logs = custodyLogRepository.findByItemIdOrderByCreatedAtDesc(itemId);
        if (logs.isEmpty()) {
            return inventoryItemRepository.findById(itemId).map(item -> {
                if (item.getQuantityInStock() > 0) {
                    return List.of(StockMovementResponse.builder()
                            .id(UuidCreatorUtils.generateUuidV7())
                            .createdAt(item.getCreatedAt() != null ? item.getCreatedAt() : java.time.LocalDateTime.now())
                            .eventType("STOCK_ENTRY")
                            .eventDescription("Saldo Inicial / Implantação")
                            .quantity(item.getQuantityInStock())
                            .balanceAfter(item.getQuantityInStock())
                            .notes("Saldo inicial cadastrado no sistema")
                            .build());
                }
                return List.<StockMovementResponse>of();
            }).orElse(List.of());
        }

        return logs.stream().map(logEntry -> {
            String desc = formatEventDescription(logEntry.getEventType());
            String warehouseName = null;
            UUID wId = logEntry.getToWarehouseId() != null ? logEntry.getToWarehouseId() : logEntry.getFromWarehouseId();
            if (wId != null) {
                warehouseName = warehouseRepository.findById(wId).map(Warehouse::getName).orElse(null);
            }

            String userName = null;
            UUID uId = logEntry.getToUserId() != null ? logEntry.getToUserId() : logEntry.getFromUserId();
            if (uId != null) {
                userName = userRepository.findById(uId).map(User::getName).orElse(null);
            }

            return StockMovementResponse.builder()
                    .id(logEntry.getId())
                    .createdAt(logEntry.getCreatedAt())
                    .eventType(logEntry.getEventType())
                    .eventDescription(desc)
                    .quantity(logEntry.getQuantity())
                    .balanceAfter(logEntry.getBalanceAfter())
                    .warehouseName(warehouseName)
                    .warehouseId(wId)
                    .userName(userName)
                    .userId(uId)
                    .workOrderId(logEntry.getWorkOrderId())
                    .notes(logEntry.getNotes())
                    .photoUrl(logEntry.getPhotoUrl())
                    .build();
        }).toList();
    }

    public List<StockMovementResponse> getItemMovementsByCode(String code) {
        return inventoryItemRepository.findByCode(code)
                .map(item -> getItemMovements(item.getId()))
                .orElse(List.of());
    }

    private String formatEventDescription(String eventType) {
        if (eventType == null) return "Movimentação de Estoque";
        return switch (eventType) {
            case "STOCK_ENTRY" -> "Entrada / Compra de Fornecedor";
            case "MATERIAL_CHECKOUT_OS" -> "Saída para Ordem de Serviço";
            case "MATERIAL_RETURN_CONFORMANT" -> "Devolução de Sobra de O.S. (Conforme)";
            case "MATERIAL_RETURN_DIVERGENT" -> "Devolução de Sobra de O.S. (Com Divergência)";
            case "TRANSFER_DISPATCH" -> "Transferência Despachada";
            case "TRANSFER_RECEIPT" -> "Transferência Recebida";
            case "CHECKOUT_TO_TECH" -> "Saída para Técnico";
            case "RETURN_FROM_TECH" -> "Devolução de Técnico";
            case "INSTALLED_AT_CLIENT" -> "Instalado no Cliente";
            case "RECOVERED_FROM_CLIENT" -> "Recolhido do Cliente (Logística Reversa)";
            default -> eventType;
        };
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
