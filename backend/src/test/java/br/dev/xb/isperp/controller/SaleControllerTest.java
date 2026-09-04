package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.CreateSaleRequest;
import br.dev.xb.isperp.entity.Sale;
import br.dev.xb.isperp.mapper.SaleMapperImpl;
import br.dev.xb.isperp.service.SaleService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SaleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SaleMapperImpl.class)
class SaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SaleService saleService;

    @Test
    @DisplayName("GET /sales - Deve retornar lista de vendas mapeada em DTOs")
    void testGetAllSales() throws Exception {
        UUID saleId = UuidCreatorUtils.generateUuidV7();
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Sale sale = Sale.builder()
                .id(saleId)
                .planId(planId)
                .customerName("Carlos Drummond")
                .customerCpf("12345678901")
                .customerPhone("11987654321")
                .installationAddress("Rua das Flores, 123")
                .city("São Paulo")
                .state("SP")
                .zipCode("01001000")
                .preferredDueDate(15)
                .notificationChannel("WHATSAPP")
                .status(Sale.SaleStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(saleService.getAllSales()).thenReturn(List.of(sale));

        mockMvc.perform(get("/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(saleId.toString()))
                .andExpect(jsonPath("$[0].customerName").value("Carlos Drummond"))
                .andExpect(jsonPath("$[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$[0].planId").value(planId.toString()));
    }

    @Test
    @DisplayName("GET /sales/{id} - Sucesso")
    void testGetSaleByIdSuccess() throws Exception {
        UUID saleId = UuidCreatorUtils.generateUuidV7();
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Sale sale = Sale.builder()
                .id(saleId)
                .planId(planId)
                .customerName("Clarice Lispector")
                .customerCpf("98765432100")
                .customerPhone("21987654321")
                .installationAddress("Av. Atlântica, 500")
                .city("Rio de Janeiro")
                .state("RJ")
                .zipCode("22010000")
                .preferredDueDate(10)
                .notificationChannel("SMS")
                .status(Sale.SaleStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(saleService.getSaleById(saleId)).thenReturn(Optional.of(sale));

        mockMvc.perform(get("/sales/{id}", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saleId.toString()))
                .andExpect(jsonPath("$.customerName").value("Clarice Lispector"))
                .andExpect(jsonPath("$.city").value("Rio de Janeiro"));
    }

    @Test
    @DisplayName("GET /sales/{id} - 404 quando venda não existir")
    void testGetSaleByIdNotFound() throws Exception {
        UUID saleId = UuidCreatorUtils.generateUuidV7();
        when(saleService.getSaleById(saleId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/sales/{id}", saleId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /sales - Deve submeter nova venda com sucesso")
    void testSubmitSaleSuccess() throws Exception {
        UUID saleId = UuidCreatorUtils.generateUuidV7();
        UUID planId = UuidCreatorUtils.generateUuidV7();
        Sale created = Sale.builder()
                .id(saleId)
                .planId(planId)
                .customerName("Vinicius de Moraes")
                .customerCpf("11122233344")
                .customerPhone("21999998888")
                .installationAddress("Rua Ipanema, 10")
                .city("Rio de Janeiro")
                .state("RJ")
                .zipCode("22410000")
                .preferredDueDate(5)
                .notificationChannel("WHATSAPP")
                .status(Sale.SaleStatus.SUBMITTED)
                .createdAt(LocalDateTime.now())
                .build();

        when(saleService.submitSale(any(CreateSaleRequest.class))).thenReturn(created);

        String json = """
                {
                    "planId": "%s",
                    "customerName": "Vinicius de Moraes",
                    "customerCpf": "11122233344",
                    "customerPhone": "21999998888",
                    "installationAddress": "Rua Ipanema, 10",
                    "city": "Rio de Janeiro",
                    "state": "RJ",
                    "zipCode": "22410000",
                    "preferredDueDate": 5,
                    "notificationChannel": "WHATSAPP"
                }
                """.formatted(planId);

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saleId.toString()))
                .andExpect(jsonPath("$.customerName").value("Vinicius de Moraes"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /sales - Retorna 400 em caso de erro de validação de negócio")
    void testSubmitSaleBadRequest() throws Exception {
        UUID planId = UuidCreatorUtils.generateUuidV7();
        when(saleService.submitSale(any(CreateSaleRequest.class)))
                .thenThrow(new RuntimeException("CPF inválido para cadastro da venda"));

        String json = """
                {
                    "planId": "%s",
                    "customerName": "Vinicius de Moraes",
                    "customerCpf": "00000000000",
                    "customerPhone": "21999998888",
                    "installationAddress": "Rua Ipanema, 10",
                    "city": "Rio de Janeiro",
                    "state": "RJ",
                    "zipCode": "22410000",
                    "preferredDueDate": 5,
                    "notificationChannel": "WHATSAPP"
                }
                """.formatted(planId);

        mockMvc.perform(post("/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
