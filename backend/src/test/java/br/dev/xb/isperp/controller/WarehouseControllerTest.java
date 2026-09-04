package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.Warehouse;
import br.dev.xb.isperp.mapper.InventoryMapperImpl;
import br.dev.xb.isperp.service.WarehouseService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(InventoryMapperImpl.class)
@SuppressWarnings("null")
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WarehouseService warehouseService;

    @Test
    @DisplayName("GET /warehouses - Deve listar todos os depósitos")
    void testGetAllWarehouses() throws Exception {
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();
        Warehouse warehouse = Warehouse.builder()
                .id(warehouseId)
                .code("DEP-CENTRAL")
                .name("Almoxarifado Central")
                .city("Belém")
                .state("PA")
                .address("Av. Almirante Barroso, 1000")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(warehouseService.getAllWarehouses()).thenReturn(List.of(warehouse));

        mockMvc.perform(get("/warehouses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(warehouseId.toString()))
                .andExpect(jsonPath("$[0].code").value("DEP-CENTRAL"))
                .andExpect(jsonPath("$[0].name").value("Almoxarifado Central"))
                .andExpect(jsonPath("$[0].city").value("Belém"))
                .andExpect(jsonPath("$[0].state").value("PA"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("GET /warehouses/active - Deve listar apenas depósitos ativos")
    void testGetActiveWarehouses() throws Exception {
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();
        Warehouse warehouse = Warehouse.builder()
                .id(warehouseId)
                .code("DEP-FILIAL-01")
                .name("Filial Ananindeua")
                .city("Ananindeua")
                .state("PA")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(warehouseService.getActiveWarehouses()).thenReturn(List.of(warehouse));

        mockMvc.perform(get("/warehouses/active")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(warehouseId.toString()))
                .andExpect(jsonPath("$[0].code").value("DEP-FILIAL-01"));
    }

    @Test
    @DisplayName("GET /warehouses/{id} - Deve retornar depósito quando encontrado")
    void testGetWarehouseByIdFound() throws Exception {
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();
        Warehouse warehouse = Warehouse.builder()
                .id(warehouseId)
                .code("DEP-01")
                .name("Depósito 1")
                .city("Castanhal")
                .state("PA")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(warehouseService.getWarehouseById(warehouseId)).thenReturn(Optional.of(warehouse));

        mockMvc.perform(get("/warehouses/{id}", warehouseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(warehouseId.toString()))
                .andExpect(jsonPath("$.name").value("Depósito 1"));
    }

    @Test
    @DisplayName("GET /warehouses/{id} - Deve retornar 404 quando não encontrado")
    void testGetWarehouseByIdNotFound() throws Exception {
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();
        when(warehouseService.getWarehouseById(warehouseId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/warehouses/{id}", warehouseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /warehouses - Deve cadastrar novo depósito com sucesso")
    void testCreateWarehouse() throws Exception {
        UUID warehouseId = UuidCreatorUtils.generateUuidV7();
        Warehouse savedWarehouse = Warehouse.builder()
                .id(warehouseId)
                .code("DEP-NOVO")
                .name("Novo Depósito Marabá")
                .city("Marabá")
                .state("PA")
                .address("Folha 32, Quadra 10")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(warehouseService.createWarehouse(any(Warehouse.class))).thenReturn(savedWarehouse);

        String jsonPayload = """
                {
                    "code": "DEP-NOVO",
                    "name": "Novo Depósito Marabá",
                    "city": "Marabá",
                    "state": "PA",
                    "address": "Folha 32, Quadra 10",
                    "active": true
                }
                """;

        mockMvc.perform(post("/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(warehouseId.toString()))
                .andExpect(jsonPath("$.code").value("DEP-NOVO"))
                .andExpect(jsonPath("$.city").value("Marabá"));
    }
}
