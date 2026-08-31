package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.ContractTemplateResponse;
import br.dev.xb.isperp.service.ContractTemplateEngine;
import br.dev.xb.isperp.service.ContractTemplateService;
import br.dev.xb.isperp.signature.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ContractTemplateController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContractTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContractTemplateService templateService;

    @MockitoBean
    private ContractTemplateEngine templateEngine;

    @Test
    @DisplayName("GET /api/contracts/templates deve retornar lista de templates")
    void testListTemplates() throws Exception {
        ContractTemplateResponse response = ContractTemplateResponse.builder()
                .id(UUID.randomUUID())
                .name("Contrato SCM Fibra")
                .documentType(DocumentType.SERVICE_AGREEMENT)
                .version(1)
                .isActive(true)
                .build();

        when(templateService.listTemplates(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/contracts/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Contrato SCM Fibra"))
                .andExpect(jsonPath("$[0].documentType").value("SERVICE_AGREEMENT"));
    }

    @Test
    @DisplayName("GET /api/contracts/templates/variables deve retornar catálogo de variáveis dinâmicas")
    void testGetVariables() throws Exception {
        when(templateService.getAvailableVariables()).thenReturn(List.of());

        mockMvc.perform(get("/api/contracts/templates/variables"))
                .andExpect(status().isOk());
    }
}
