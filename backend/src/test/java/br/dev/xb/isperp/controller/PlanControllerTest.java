package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.mapper.PlanMapperImpl;
import br.dev.xb.isperp.service.PlanService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PlanMapperImpl.class)
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanService planService;

    @Test
    @DisplayName("GET /plans - Deve retornar lista de planos mapeada em DTOs")
    void testGetAllPlans() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 500 Mega")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(BigDecimal.valueOf(99.90))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(planService.getAllPlans()).thenReturn(List.of(plan));

        mockMvc.perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(planId.toString()))
                .andExpect(jsonPath("$[0].name").value("Fibra 500 Mega"))
                .andExpect(jsonPath("$[0].downloadSpeed").value(500))
                .andExpect(jsonPath("$[0].uploadSpeed").value(250))
                .andExpect(jsonPath("$[0].price").value(99.90))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("GET /plans/active - Deve retornar apenas planos ativos")
    void testGetActivePlans() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 1 Giga")
                .downloadSpeed(1000)
                .uploadSpeed(500)
                .price(BigDecimal.valueOf(149.90))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(planService.getActivePlans()).thenReturn(List.of(plan));

        mockMvc.perform(get("/plans/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(planId.toString()))
                .andExpect(jsonPath("$[0].name").value("Fibra 1 Giga"));
    }

    @Test
    @DisplayName("GET /plans/{id} - Sucesso")
    void testGetPlanByIdSuccess() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 300 Mega")
                .downloadSpeed(300)
                .uploadSpeed(150)
                .price(BigDecimal.valueOf(79.90))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(planService.getPlanById(planId)).thenReturn(Optional.of(plan));

        mockMvc.perform(get("/plans/{id}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.name").value("Fibra 300 Mega"));
    }

    @Test
    @DisplayName("GET /plans/{id} - 404 quando plano não existir")
    void testGetPlanByIdNotFound() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        when(planService.getPlanById(planId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/plans/{id}", planId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /plans - Deve cadastrar novo plano")
    void testCreatePlan() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Plan created = Plan.builder()
                .id(planId)
                .name("Fibra Gamer 600 Mega")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .price(BigDecimal.valueOf(119.90))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(planService.createPlan(any(Plan.class))).thenReturn(created);

        String json = """
                {
                    "name": "Fibra Gamer 600 Mega",
                    "downloadSpeed": 600,
                    "uploadSpeed": 300,
                    "price": 119.90,
                    "active": true
                }
                """;

        mockMvc.perform(post("/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.name").value("Fibra Gamer 600 Mega"));
    }

    @Test
    @DisplayName("PUT /plans/{id} - Deve atualizar plano existente")
    void testUpdatePlan() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Plan updated = Plan.builder()
                .id(planId)
                .name("Fibra 700 Mega Atualizada")
                .downloadSpeed(700)
                .uploadSpeed(350)
                .price(BigDecimal.valueOf(129.90))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(planService.updatePlan(eq(planId), any(Plan.class))).thenReturn(updated);

        String json = """
                {
                    "name": "Fibra 700 Mega Atualizada",
                    "downloadSpeed": 700,
                    "uploadSpeed": 350,
                    "price": 129.90,
                    "active": true
                }
                """;

        mockMvc.perform(put("/plans/{id}", planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fibra 700 Mega Atualizada"));
    }

    @Test
    @DisplayName("DELETE /plans/{id} - Deve deletar com sucesso")
    void testDeletePlanSuccess() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        doNothing().when(planService).deletePlan(planId);

        mockMvc.perform(delete("/plans/{id}", planId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /plans/{id} - 404 quando plano não existir")
    void testDeletePlanNotFound() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        doThrow(new RuntimeException("Plano não encontrado")).when(planService).deletePlan(planId);

        mockMvc.perform(delete("/plans/{id}", planId))
                .andExpect(status().isNotFound());
    }
}
