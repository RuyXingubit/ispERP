package br.dev.xb.isperp.fiscal;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.fiscal.dto.*;
import br.dev.xb.isperp.fiscal.xingubit.XingubitPayFiscalDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class XingubitPayFiscalDriverTest {

    private XingubitPayFiscalDriver driver;
    private FiscalCompany company;
    private FiscalGatewayConfig config;

    @BeforeEach
    void setUp() {
        driver = new XingubitPayFiscalDriver();

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

    @Test
    @DisplayName("Deve registrar os dados fiscais da empresa no driver")
    void testRegisterCompany() {
        boolean registered = driver.registerCompany(company, config);
        assertTrue(registered);
    }

    @Test
    @DisplayName("Deve parametrizar a série de NFCom no driver")
    void testConfigureNfcom() {
        boolean configured = driver.configureNfcom(company, config);
        assertTrue(configured);
    }

    @Test
    @DisplayName("Deve fazer upload do certificado A1 com resposta de sucesso")
    void testUploadCertificate() {
        byte[] fakePfx = "FAKE_PFX_BINARY_CONTENT".getBytes();
        CertificateUploadResult result = driver.uploadCertificate(fakePfx, "senha123", config);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getSubjectName());
        assertNotNull(result.getValidUntil());
    }

    @Test
    @DisplayName("Deve emitir NFCom (Modelo 62) gerando chave de 44 dígitos e protocolo")
    void testIssueNfcom() {
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
        assertNotNull(result.getChaveAcesso());
        assertEquals(44, result.getChaveAcesso().length(), "Chave de acesso da NFCom deve ter exatamente 44 dígitos");
        assertNotNull(result.getProtocoloAutorizacao());
        assertNotNull(result.getDanfePdfUrl());
        assertNotNull(result.getXmlUrl());
    }

    @Test
    @DisplayName("Deve consultar status e cancelar NFCom")
    void testQueryAndCancelNfcom() {
        String chave = "15260812345678000195620010000001011123456789";

        NfcomStatusResult statusRes = driver.queryStatus(chave, config);
        assertTrue(statusRes.isSuccess());
        assertEquals("AUTORIZADA", statusRes.getStatus());

        NfcomCancelResult cancelRes = driver.cancelNfcom(chave, "Cancelamento a pedido do assinante", config);
        assertTrue(cancelRes.isSuccess());
        assertNotNull(cancelRes.getProtocoloCancelamento());
    }
}
