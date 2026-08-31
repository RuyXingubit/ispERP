package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.SignaturePublicViewResponse;
import br.dev.xb.isperp.dto.SignatureSessionResponse;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import br.dev.xb.isperp.signature.SignatureStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ElectronicSignatureController.class, ElectronicSignatureWebhookController.class})
@AutoConfigureMockMvc(addFilters = false)
class ElectronicSignatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ElectronicSignatureService signatureService;

    @Test
    @DisplayName("GET /api/public/signatures/{token} deve retornar visualização pública para o assinante")
    void testGetPublicSignatureView() throws Exception {
        SignaturePublicViewResponse response = SignaturePublicViewResponse.builder()
                .token("tok12345")
                .contractName("Contrato nº CTR-2026-0001")
                .customerName("José da Silva")
                .customerDocumentMasked("***.123.456-**")
                .companyName("Xingubit Telecom")
                .renderedContent("# Termos do Contrato")
                .consentClause("Aceito os termos pagando o Pix.")
                .status(SignatureStatus.PENDING)
                .symbolicAmount(BigDecimal.valueOf(1.00))
                .pixCopyPaste("00020101...")
                .expiresAt(OffsetDateTime.now().plusHours(72))
                .build();

        when(signatureService.getPublicSignatureView(eq("tok12345"), any(), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/public/signatures/tok12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok12345"))
                .andExpect(jsonPath("$.customerName").value("José da Silva"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.symbolicAmount").value(1.00));
    }

    @Test
    @DisplayName("GET /api/public/signatures/{token}/status deve retornar status atualizado da sessão")
    void testGetSignatureStatus() throws Exception {
        SignatureSessionResponse response = SignatureSessionResponse.builder()
                .id(UUID.randomUUID())
                .token("tok12345")
                .status(SignatureStatus.SIGNED)
                .pixEndToEndId("E12345678")
                .build();

        when(signatureService.getSignatureStatus("tok12345")).thenReturn(response);

        mockMvc.perform(get("/api/public/signatures/tok12345/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"))
                .andExpect(jsonPath("$.pixEndToEndId").value("E12345678"));
    }
}
