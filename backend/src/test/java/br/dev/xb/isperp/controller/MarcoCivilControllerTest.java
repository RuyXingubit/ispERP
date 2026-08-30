package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.service.MarcoCivilInvestigationService;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {MarcoCivilController.class, PublicVerificationController.class})
@AutoConfigureMockMvc(addFilters = false)
class MarcoCivilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarcoCivilInvestigationService marcoCivilInvestigationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/marco-civil/search - Deve realizar busca de assinante")
    void testSearchSubscriber() throws Exception {
        MarcoCivilSearchRequest req = MarcoCivilSearchRequest.builder()
                .ip("200.150.10.2")
                .port(1500)
                .timestamp(OffsetDateTime.now())
                .build();

        MarcoCivilSearchResult res = MarcoCivilSearchResult.builder()
                .matched(true)
                .queriedIp("200.150.10.2")
                .queriedPort(1500)
                .customerName("Ana Paula Silva")
                .build();

        when(marcoCivilInvestigationService.searchSubscriber(any())).thenReturn(res);

        mockMvc.perform(post("/api/marco-civil/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.customerName").value("Ana Paula Silva"));
    }

    @Test
    @DisplayName("POST /api/marco-civil/reports - Deve gerar laudo oficial com hash")
    void testGenerateOfficialReport() throws Exception {
        MarcoCivilReportRequest req = MarcoCivilReportRequest.builder()
                .courtOrderNumber("OF-999")
                .queriedIp("200.150.10.2")
                .queriedPort(1500)
                .queriedTimestamp(OffsetDateTime.now())
                .build();

        MarcoCivilReportResponse res = MarcoCivilReportResponse.builder()
                .id(UUID.randomUUID())
                .validationToken("tok999")
                .sha256Hash("hash-sha256-val")
                .publicValidationUrl("http://localhost:5173/public/validar-laudo/tok999")
                .build();

        when(marcoCivilInvestigationService.generateOfficialReport(any())).thenReturn(res);

        mockMvc.perform(post("/api/marco-civil/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validationToken").value("tok999"))
                .andExpect(jsonPath("$.sha256Hash").value("hash-sha256-val"));
    }

    @Test
    @DisplayName("GET /api/public/marco-civil/validate/{token} - Deve validar token de laudo")
    void testValidateToken() throws Exception {
        PublicValidationResponse res = PublicValidationResponse.builder()
                .valid(true)
                .validationToken("tok999")
                .statusMessage("DOCUMENTO AUTÊNTICO")
                .build();

        when(marcoCivilInvestigationService.validatePublicToken("tok999")).thenReturn(res);

        mockMvc.perform(get("/api/public/marco-civil/validate/tok999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.statusMessage").value("DOCUMENTO AUTÊNTICO"));
    }
}
