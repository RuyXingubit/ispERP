package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.CgnatMappingResponse;
import br.dev.xb.isperp.dto.CgnatScriptImportRequest;
import br.dev.xb.isperp.dto.CgnatScriptImportResponse;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.service.CgnatParserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CgnatController.class)
@AutoConfigureMockMvc(addFilters = false)
class CgnatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CgnatParserService cgnatParserService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/cgnat/mappings - Deve listar todos os mapeamentos")
    void testGetAllMappings() throws Exception {
        CgnatMappingResponse res = CgnatMappingResponse.builder()
                .id(UUID.randomUUID())
                .publicIp("200.150.10.2")
                .portStart(1024)
                .portEnd(2047)
                .privateIpStart("100.64.1.2")
                .privateIpEnd("100.64.1.2")
                .vendorType(NasVendorType.MIKROTIK)
                .build();

        when(cgnatParserService.getAllMappings()).thenReturn(List.of(res));

        mockMvc.perform(get("/api/cgnat/mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].publicIp").value("200.150.10.2"))
                .andExpect(jsonPath("$[0].portStart").value(1024));
    }

    @Test
    @DisplayName("POST /api/cgnat/import-script - Deve processar e importar regras")
    void testImportScript() throws Exception {
        CgnatScriptImportRequest req = CgnatScriptImportRequest.builder()
                .vendorType(NasVendorType.MIKROTIK)
                .scriptContent("/ip firewall nat add chain=srcnat action=src-nat src-address=100.64.1.2 to-addresses=200.150.10.2 to-ports=1000-1999 protocol=tcp")
                .build();

        CgnatScriptImportResponse res = CgnatScriptImportResponse.builder()
                .totalParsed(1)
                .totalSaved(1)
                .importedMappings(List.of())
                .warnings(List.of())
                .build();

        when(cgnatParserService.importScript(any())).thenReturn(res);

        mockMvc.perform(post("/api/cgnat/import-script")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParsed").value(1))
                .andExpect(jsonPath("$.totalSaved").value(1));
    }
}
