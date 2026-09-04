package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.entity.NfcomRecord;
import br.dev.xb.isperp.fiscal.FiscalGatewayType;
import br.dev.xb.isperp.fiscal.dto.NfcomCancelResult;
import br.dev.xb.isperp.mapper.FiscalMapperImpl;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.FiscalGatewayConfigRepository;
import br.dev.xb.isperp.scheduler.FiscalAccountingScheduler;
import br.dev.xb.isperp.service.ConvenioIcms115Service;
import br.dev.xb.isperp.service.FiscalService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FiscalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(FiscalMapperImpl.class)
@SuppressWarnings("null")
class FiscalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FiscalService fiscalService;

    @MockitoBean
    private ConvenioIcms115Service convenio115Service;

    @MockitoBean
    private FiscalCompanyRepository companyRepository;

    @MockitoBean
    private FiscalGatewayConfigRepository configRepository;

    @MockitoBean
    private FiscalAccountingScheduler fiscalAccountingScheduler;

    @Test
    @DisplayName("GET /fiscal/company - Deve retornar empresa fiscal ativa mapeada em DTO com UUIDv7")
    void testGetActiveCompany() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        FiscalCompany company = FiscalCompany.builder()
                .id(companyId)
                .cnpj("12.345.678/0001-90")
                .razaoSocial("Nexus Fibra Telecomunicacoes LTDA")
                .nomeFantasia("Nexus Fibra")
                .inscricaoEstadual("15.123.456-7")
                .cnaePrincipal("6110-8/03")
                .regimeTributario("SIMPLES_NACIONAL")
                .logradouro("Avenida Brasil")
                .numero("1000")
                .bairro("Centro")
                .cidade("Santarém")
                .uf("PA")
                .cep("68005-000")
                .codigoIbge("1506807")
                .nfcomAmbiente("HOMOLOGACAO")
                .nfcomSerie("1")
                .nfcomProximoNumero(101)
                .hasCertificate(true)
                .fiscalConfirmed(true)
                .isActive(true)
                .build();

        when(companyRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(company));

        mockMvc.perform(get("/fiscal/company")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyId.toString()))
                .andExpect(jsonPath("$.cnpj").value("12.345.678/0001-90"))
                .andExpect(jsonPath("$.razaoSocial").value("Nexus Fibra Telecomunicacoes LTDA"))
                .andExpect(jsonPath("$.regimeTributario").value("SIMPLES_NACIONAL"))
                .andExpect(jsonPath("$.nfcomAmbiente").value("HOMOLOGACAO"))
                .andExpect(jsonPath("$.hasCertificate").value(true));
    }

    @Test
    @DisplayName("GET /fiscal/configs - Deve retornar config do gateway fiscal mascarando segredos")
    void testGetActiveConfig() throws Exception {
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        FiscalGatewayConfig config = FiscalGatewayConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .companyId(companyId)
                .gatewayType(FiscalGatewayType.XINGUBIT_PAY)
                .environment("HOMOLOGACAO")
                .baseUrl("https://pay.xingubit.com.br")
                .clientSecret("SUPER_SECRET_CLIENT_SECRET")
                .apiKey("SUPER_SECRET_API_KEY")
                .webhookSecret("SUPER_SECRET_WEBHOOK")
                .isActive(true)
                .build();

        when(configRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(config));

        mockMvc.perform(get("/fiscal/configs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.gatewayType").value("XINGUBIT_PAY"))
                .andExpect(jsonPath("$.environment").value("HOMOLOGACAO"))
                .andExpect(jsonPath("$.baseUrl").value("https://pay.xingubit.com.br"))
                .andExpect(jsonPath("$.clientSecret").doesNotExist())
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.webhookSecret").doesNotExist());
    }

    @Test
    @DisplayName("POST /fiscal/invoices/{invoiceId}/emit - Deve emitir NFCom Modelo 62")
    void testEmitNfcom() throws Exception {
        UUID invoiceId = UuidCreatorUtils.generateUuidV7();
        UUID recordId = UuidCreatorUtils.generateUuidV7();
        UUID companyId = UuidCreatorUtils.generateUuidV7();
        UUID customerId = UuidCreatorUtils.generateUuidV7();

        NfcomRecord record = NfcomRecord.builder()
                .id(recordId)
                .companyId(companyId)
                .invoiceId(invoiceId)
                .customerId(customerId)
                .chaveAcesso("15260912345678000190620010000001011234567890")
                .numero(101)
                .serie("1")
                .modelo("62")
                .tipoEmissao("NORMAL")
                .ambiente("HOMOLOGACAO")
                .status("AUTORIZADA")
                .protocoloAutorizacao("115260000012345")
                .valorTotal(new BigDecimal("99.90"))
                .valorIcms(BigDecimal.ZERO)
                .danfePdfUrl("https://isperp.local/danfe/101.pdf")
                .createdAt(LocalDateTime.now())
                .build();

        when(fiscalService.issueNfcomForInvoice(invoiceId)).thenReturn(record);

        mockMvc.perform(post("/fiscal/invoices/{invoiceId}/emit", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(recordId.toString()))
                .andExpect(jsonPath("$.chaveAcesso").value("15260912345678000190620010000001011234567890"))
                .andExpect(jsonPath("$.numero").value(101))
                .andExpect(jsonPath("$.status").value("AUTORIZADA"))
                .andExpect(jsonPath("$.valorTotal").value(99.90));
    }

    @Test
    @DisplayName("GET /fiscal/records - Deve retornar página de registros de NFCom")
    void testListNfcomRecords() throws Exception {
        UUID recordId = UuidCreatorUtils.generateUuidV7();
        NfcomRecord record = NfcomRecord.builder()
                .id(recordId)
                .companyId(UuidCreatorUtils.generateUuidV7())
                .customerId(UuidCreatorUtils.generateUuidV7())
                .numero(102)
                .serie("1")
                .modelo("62")
                .tipoEmissao("NORMAL")
                .ambiente("HOMOLOGACAO")
                .status("AUTORIZADA")
                .valorTotal(new BigDecimal("149.90"))
                .createdAt(LocalDateTime.now())
                .build();

        PageImpl<NfcomRecord> page = new PageImpl<>(List.of(record));
        when(fiscalService.listRecords(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/fiscal/records?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(recordId.toString()))
                .andExpect(jsonPath("$.content[0].numero").value(102))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /fiscal/records/{recordId}/cancel - Deve cancelar NFCom autorizada")
    void testCancelNfcom() throws Exception {
        UUID recordId = UuidCreatorUtils.generateUuidV7();
        NfcomCancelResult cancelResult = NfcomCancelResult.builder()
                .success(true)
                .chaveAcesso("15260912345678000190620010000001011234567890")
                .protocoloCancelamento("115260000099999")
                .dataCancelamento(LocalDateTime.now())
                .build();

        when(fiscalService.cancelNfcom(eq(recordId), any(String.class))).thenReturn(cancelResult);

        mockMvc.perform(post("/fiscal/records/{recordId}/cancel", recordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cancelamento solicitado por teste unitário\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.protocoloCancelamento").value("115260000099999"));
    }
}
