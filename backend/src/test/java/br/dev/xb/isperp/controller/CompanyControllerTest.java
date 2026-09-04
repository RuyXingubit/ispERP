package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.Company;
import br.dev.xb.isperp.mapper.CompanyMapperImpl;
import br.dev.xb.isperp.service.CompanyService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompanyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CompanyMapperImpl.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @Test
    @DisplayName("GET /companies - Deve retornar lista de empresas mapeada em DTOs")
    void testGetAllCompanies() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        Company company = Company.builder()
                .id(companyId)
                .name("Telecom Provedor Ltda")
                .document("12.345.678/0001-90")
                .address("Av. Central, 100")
                .phone("11988887777")
                .email("contato@provedor.com.br")
                .website("https://provedor.com.br")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(companyService.getAllCompanies()).thenReturn(List.of(company));

        mockMvc.perform(get("/companies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(companyId.toString()))
                .andExpect(jsonPath("$[0].name").value("Telecom Provedor Ltda"))
                .andExpect(jsonPath("$[0].document").value("12.345.678/0001-90"))
                .andExpect(jsonPath("$[0].email").value("contato@provedor.com.br"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("GET /companies/primary - Deve retornar a empresa principal (matriz)")
    void testGetPrimaryCompanySuccess() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        Company company = Company.builder()
                .id(companyId)
                .name("Matriz Telecom Provedor")
                .document("12.345.678/0001-90")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(companyService.getPrimaryCompany()).thenReturn(Optional.of(company));

        mockMvc.perform(get("/companies/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()))
                .andExpect(jsonPath("$.name").value("Matriz Telecom Provedor"));
    }

    @Test
    @DisplayName("GET /companies/primary - 404 quando nenhuma empresa cadastrada")
    void testGetPrimaryCompanyNotFound() throws Exception {
        when(companyService.getPrimaryCompany()).thenReturn(Optional.empty());

        mockMvc.perform(get("/companies/primary"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /companies/{id} - Sucesso")
    void testGetCompanyByIdSuccess() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        Company company = Company.builder()
                .id(companyId)
                .name("Filial Zona Sul")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(companyService.getCompanyById(companyId)).thenReturn(Optional.of(company));

        mockMvc.perform(get("/companies/{id}", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()))
                .andExpect(jsonPath("$.name").value("Filial Zona Sul"));
    }

    @Test
    @DisplayName("GET /companies/{id} - 404 quando empresa não existir")
    void testGetCompanyByIdNotFound() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        when(companyService.getCompanyById(companyId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/companies/{id}", companyId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /companies - Deve cadastrar nova empresa/filial")
    void testCreateCompany() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        Company created = Company.builder()
                .id(companyId)
                .name("Filial Zona Norte")
                .document("12.345.678/0002-71")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(companyService.createCompany(any(Company.class))).thenReturn(created);

        String json = """
                {
                    "name": "Filial Zona Norte",
                    "document": "12.345.678/0002-71",
                    "active": true
                }
                """;

        mockMvc.perform(post("/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(companyId.toString()))
                .andExpect(jsonPath("$.name").value("Filial Zona Norte"));
    }

    @Test
    @DisplayName("PUT /companies/{id} - Deve atualizar dados de empresa")
    void testUpdateCompany() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        Company updated = Company.builder()
                .id(companyId)
                .name("Filial Zona Norte Alterada")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(companyService.updateCompany(eq(companyId), any(Company.class))).thenReturn(updated);

        String json = """
                {
                    "name": "Filial Zona Norte Alterada",
                    "active": true
                }
                """;

        mockMvc.perform(put("/companies/{id}", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Filial Zona Norte Alterada"));
    }

    @Test
    @DisplayName("DELETE /companies/{id} - Deve deletar com sucesso")
    void testDeleteCompanySuccess() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        doNothing().when(companyService).deleteCompany(companyId);

        mockMvc.perform(delete("/companies/{id}", companyId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /companies/{id} - 404 quando empresa não existir")
    void testDeleteCompanyNotFound() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        doThrow(new RuntimeException("Empresa não encontrada")).when(companyService).deleteCompany(companyId);

        mockMvc.perform(delete("/companies/{id}", companyId))
                .andExpect(status().isNotFound());
    }
}
