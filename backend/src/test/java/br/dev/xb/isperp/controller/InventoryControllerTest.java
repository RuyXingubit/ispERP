package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.InventoryItem;
import br.dev.xb.isperp.mapper.InventoryMapperImpl;
import br.dev.xb.isperp.service.InventoryService;
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
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(InventoryMapperImpl.class)
@SuppressWarnings("null")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    @DisplayName("GET /inventory - Deve retornar lista de insumos e saldo de estoque mapeados em DTO")
    void testGetAllInventoryItems() throws Exception {
        UUID itemId = UuidCreatorUtils.generateUuidV7();
        InventoryItem item = InventoryItem.builder()
                .id(itemId)
                .code("ONT-WIFI6-XPON")
                .name("ONT Wi-Fi 6 XPON Gigabit")
                .category("ONU_ONT")
                .quantityInStock(50)
                .minQuantity(10)
                .unit("UN")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(inventoryService.getAllItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/inventory")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$[0].code").value("ONT-WIFI6-XPON"))
                .andExpect(jsonPath("$[0].name").value("ONT Wi-Fi 6 XPON Gigabit"))
                .andExpect(jsonPath("$[0].category").value("ONU_ONT"))
                .andExpect(jsonPath("$[0].quantityInStock").value(50))
                .andExpect(jsonPath("$[0].minQuantity").value(10))
                .andExpect(jsonPath("$[0].unit").value("UN"));
    }
}
