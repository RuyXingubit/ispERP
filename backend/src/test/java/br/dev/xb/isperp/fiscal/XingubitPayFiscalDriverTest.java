package br.dev.xb.isperp.fiscal;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.fiscal.dto.*;
import br.dev.xb.isperp.fiscal.xingubit.XingubitPayFiscalDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class XingubitPayFiscalDriverTest {

    private MockRestServiceServer mockServer;
    private XingubitPayFiscalDriver driver;
    private FiscalCompany company;
    private FiscalGatewayConfig config;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        driver = new XingubitPayFiscalDriver(restClientBuilder);

        company = FiscalCompany.builder()
                .id(UUID.randomUUID())
                .cnpj("12.345.678/0001-95")
                .razaoSocial("Xingu Telecom Provedor de Internet Ltda")
                .inscricaoEstadual("15999888")
                .cnaePrincipal("6110-8/03")
                .regimeTributario("SIMPLES_NACIONAL")
                .logradouro("Av. Tancredo Neves")
                .numero("1500")
                .bairro("Centro")
                .cidade("Altamira")
                .uf("PA")
                .cep("68370-000")
                .codigoIbge("1500602")
                .nfcomAmbiente("HOMOLOGACAO")
                .nfcomSerie("1")
                .nfcomProximoNumero(101)
                .build();

        config = FiscalGatewayConfig.builder()
                .companyId(company.getId())
                .gatewayType(FiscalGatewayType.XINGUBIT_PAY)
                .environment("HOMOLOGACAO")
                .baseUrl("https://pay.xingubit.com.br")
                .clientId("test_client")
                .clientSecret("test_secret")
                .build();
    }

    private void mockAuthToken() {
        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"mock_token_abc\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("Deve registrar os dados fiscais da empresa no driver via API REST real")
    void testRegisterCompany() {
        mockAuthToken();
        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/empresas"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock_token_abc"))
                .andRespond(withSuccess());

        boolean registered = driver.registerCompany(company, config);
        assertTrue(registered);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve retornar false quando falhar registro da empresa")
    void testRegisterCompanyFailure() {
        mockAuthToken();
        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/empresas"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        boolean registered = driver.registerCompany(company, config);
        assertFalse(registered);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve parametrizar a série de NFCom no driver via API REST real")
    void testConfigureNfcom() {
        mockAuthToken();
        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/empresas/12345678000195/config/nfcom"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer mock_token_abc"))
                .andRespond(withSuccess());

        boolean configured = driver.configureNfcom(company, config);
        assertTrue(configured);
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve fazer upload do certificado A1 com resposta real do gateway")
    void testUploadCertificate() {
        mockAuthToken();
        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/merchants/me/certificate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock_token_abc"))
                .andRespond(withSuccess("{\"cnpj\":\"12.345.678/0001-95\",\"razaoSocial\":\"Xingu Telecom\",\"validUntil\":\"2027-12-31T23:59:59\"}", MediaType.APPLICATION_JSON));

        byte[] fakePfx = "FAKE_PFX_BINARY_CONTENT".getBytes();
        CertificateUploadResult result = driver.uploadCertificate(fakePfx, "senha123", config);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("12.345.678/0001-95", result.getSubjectCnpj());
        assertEquals("Xingu Telecom", result.getSubjectName());
        assertNotNull(result.getValidUntil());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve emitir NFCom (Modelo 62) consumindo resposta autorizada da API")
    void testIssueNfcom() {
        mockAuthToken();
        String jsonResponse = "{\"status\":\"AUTORIZADA\",\"id\":\"XINGUBIT-NFCOM-101\",\"chaveAcesso\":\"15260812345678000195620010000001011123456789\",\"protocoloAutorizacao\":\"115260000001234\",\"digestValue\":\"abc123digest=\",\"danfePdfUrl\":\"https://cdn.pay.xingubit.com.br/danfe.pdf\",\"xmlUrl\":\"https://cdn.pay.xingubit.com.br/danfe.xml\"}";

        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/invoices/nfcom"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock_token_abc"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        NfcomIssueRequest request = NfcomIssueRequest.builder()
                .invoiceId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .customerName("João Silva")
                .customerDocument("529.982.247-25")
                .customerStreet("Rua 10")
                .customerNumber("50")
                .customerNeighborhood("Centro")
                .customerCity("Altamira")
                .customerState("PA")
                .customerZipCode("68370-000")
                .customerIbgeCode("1500602")
                .totalAmount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().plusDays(10))
                .description("Internet Fibra Óptica 500 Mega")
                .build();

        NfcomIssueResult result = driver.issueNfcom(request, company, config);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("AUTORIZADA", result.getStatus());
        assertEquals("15260812345678000195620010000001011123456789", result.getChaveAcesso());
        assertEquals(44, result.getChaveAcesso().length());
        assertEquals("115260000001234", result.getProtocoloAutorizacao());
        assertEquals("https://cdn.pay.xingubit.com.br/danfe.pdf", result.getDanfePdfUrl());
        assertEquals("https://cdn.pay.xingubit.com.br/danfe.xml", result.getXmlUrl());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve consultar status da NFCom via endpoint da API remota")
    void testQueryStatus() {
        String chave = "15260812345678000195620010000001011123456789";
        mockAuthToken();
        String statusJson = "{\"status\":\"AUTORIZADA\",\"chaveAcesso\":\"" + chave + "\",\"protocoloAutorizacao\":\"115260000001234\",\"danfePdfUrl\":\"https://cdn.pay.xingubit.com.br/danfe.pdf\",\"xmlUrl\":\"https://cdn.pay.xingubit.com.br/danfe.xml\"}";
        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/invoices/nfcom/" + chave))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer mock_token_abc"))
                .andRespond(withSuccess(statusJson, MediaType.APPLICATION_JSON));

        NfcomStatusResult statusRes = driver.queryStatus(chave, config);
        assertTrue(statusRes.isSuccess());
        assertEquals("AUTORIZADA", statusRes.getStatus());
        mockServer.verify();
    }

    @Test
    @DisplayName("Deve cancelar NFCom via endpoint da API remota")
    void testCancelNfcom() {
        String chave = "15260812345678000195620010000001011123456789";
        mockAuthToken();
        String cancelJson = "{\"status\":\"CANCELADA\",\"protocoloCancelamento\":\"115269991234567\"}";
        mockServer.expect(requestTo("https://pay.xingubit.com.br/v1/invoices/nfcom/" + chave + "/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock_token_abc"))
                .andRespond(withSuccess(cancelJson, MediaType.APPLICATION_JSON));

        NfcomCancelResult cancelRes = driver.cancelNfcom(chave, "Cancelamento a pedido do assinante", config);
        assertTrue(cancelRes.isSuccess());
        assertEquals("115269991234567", cancelRes.getProtocoloCancelamento());
        mockServer.verify();
    }
}
