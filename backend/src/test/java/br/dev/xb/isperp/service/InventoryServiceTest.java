package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.StockMovementResponse;
import br.dev.xb.isperp.entity.CustodyLog;
import br.dev.xb.isperp.entity.InventoryItem;
import br.dev.xb.isperp.entity.Warehouse;
import br.dev.xb.isperp.repository.CustodyLogRepository;
import br.dev.xb.isperp.repository.InventoryItemRepository;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.WarehouseRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private CustodyLogRepository custodyLogRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID warehouseId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        warehouseId = UuidCreatorUtils.generateUuidV7();
        itemId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve registrar entrada cumulativa de estoque (+10 e depois +5 = 15) com log de custódia Kardex")
    void shouldRegisterCumulativeStockEntriesWithCustodyLog() {
        InventoryItem existingItem = InventoryItem.builder()
                .id(itemId)
                .code("DROP-1FO-1000M")
                .name("Bobina de Cabo Drop Óptico 1FO 1000m")
                .category("CABO_DROP")
                .quantityInStock(10)
                .minQuantity(5)
                .unit("BOB")
                .build();

        when(inventoryItemRepository.findByCode("DROP-1FO-1000M")).thenReturn(Optional.of(existingItem));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(i -> i.getArgument(0));
        when(custodyLogRepository.save(any(CustodyLog.class))).thenAnswer(i -> i.getArgument(0));

        // Segunda entrada de +5 bobinas
        InventoryItem updated = inventoryService.registerStockEntry(
                warehouseId,
                "DROP-1FO-1000M",
                "Bobina de Cabo Drop Óptico 1FO 1000m",
                "CABO_DROP",
                5,
                "BOB",
                "NF 9821 - Distribuidora Fibra"
        );

        assertNotNull(updated);
        assertEquals(15, updated.getQuantityInStock(), "Saldo cumulativo deve ser 15 (10 + 5)");
        verify(inventoryItemRepository, times(1)).save(existingItem);
        verify(custodyLogRepository, times(1)).save(argThat(log ->
                log.getQuantity() == 5 &&
                log.getBalanceAfter() == 15 &&
                "STOCK_ENTRY".equals(log.getEventType()) &&
                warehouseId.equals(log.getToWarehouseId())
        ));
    }

    @Test
    @DisplayName("Deve listar movimentações do item com dados de depósito e usuário resolvidos")
    void shouldGetItemMovementsWithResolvedDetails() {
        CustodyLog log1 = CustodyLog.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .itemId(itemId)
                .toWarehouseId(warehouseId)
                .eventType("STOCK_ENTRY")
                .quantity(15)
                .balanceAfter(15)
                .notes("Lote de entrada")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        Warehouse warehouse = Warehouse.builder()
                .id(warehouseId)
                .name("Depósito Central Altamira")
                .build();

        when(custodyLogRepository.findByItemIdOrderByCreatedAtDesc(itemId)).thenReturn(List.of(log1));
        when(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));

        List<StockMovementResponse> movements = inventoryService.getItemMovements(itemId);

        assertNotNull(movements);
        assertEquals(1, movements.size());
        StockMovementResponse m = movements.get(0);
        assertEquals("STOCK_ENTRY", m.getEventType());
        assertEquals("Entrada / Compra de Fornecedor", m.getEventDescription());
        assertEquals(15, m.getQuantity());
        assertEquals(15, m.getBalanceAfter());
        assertEquals("Depósito Central Altamira", m.getWarehouseName());
    }

    @Test
    @DisplayName("Deve retornar saldo inicial como movimentação caso item não possua logs prévios")
    void shouldReturnBaselineMovementWhenNoLogsExist() {
        InventoryItem item = InventoryItem.builder()
                .id(itemId)
                .code("FAST-SC-APC")
                .name("Conector Fast SC/APC")
                .quantityInStock(800)
                .unit("UN")
                .createdAt(LocalDateTime.now().minusMonths(1))
                .build();

        when(custodyLogRepository.findByItemIdOrderByCreatedAtDesc(itemId)).thenReturn(List.of());
        when(inventoryItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        List<StockMovementResponse> movements = inventoryService.getItemMovements(itemId);

        assertNotNull(movements);
        assertEquals(1, movements.size());
        assertEquals("STOCK_ENTRY", movements.get(0).getEventType());
        assertEquals(800, movements.get(0).getQuantity());
        assertEquals(800, movements.get(0).getBalanceAfter());
    }
}
