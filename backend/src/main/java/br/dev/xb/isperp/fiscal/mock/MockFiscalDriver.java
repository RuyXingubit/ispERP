package br.dev.xb.isperp.fiscal.mock;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.fiscal.FiscalGateway;
import br.dev.xb.isperp.fiscal.FiscalGatewayType;
import br.dev.xb.isperp.fiscal.dto.*;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@SuppressWarnings("null")
public class MockFiscalDriver implements FiscalGateway {

    @Override
    public FiscalGatewayType getGatewayType() {
        return FiscalGatewayType.MOCK;
    }

    @Override
    public boolean registerCompany(FiscalCompany company, FiscalGatewayConfig config) {
        return true;
    }

    @Override
    public boolean configureNfcom(FiscalCompany company, FiscalGatewayConfig config) {
        return true;
    }

    @Override
    public CertificateUploadResult uploadCertificate(byte[] pfxBytes, String password, FiscalGatewayConfig config) {
        return CertificateUploadResult.builder()
                .success(true)
                .subjectCnpj("12.345.678/0001-95")
                .subjectName("Mock ISP Telecom LTDA")
                .validUntil(LocalDateTime.now().plusYears(1))
                .message("Certificado mock instalado com sucesso.")
                .build();
    }

    @Override
    public NfcomIssueResult issueNfcom(NfcomIssueRequest request, FiscalCompany company, FiscalGatewayConfig config) {
        String accessKey = "15260812345678000195620010000000011123456789";
        return NfcomIssueResult.builder()
                .success(true)
                .externalId("MOCK-NFCOM-001")
                .chaveAcesso(accessKey)
                .numero(company.getNfcomProximoNumero())
                .serie(company.getNfcomSerie())
                .status("AUTORIZADA")
                .protocoloAutorizacao("115260001112223")
                .dataAutorizacao(LocalDateTime.now())
                .digestValue("mockDigest==")
                .danfePdfUrl("https://pay.xingubit.com.br/v1/invoices/nfcom/" + accessKey + "/pdf")
                .xmlUrl("https://pay.xingubit.com.br/v1/invoices/nfcom/" + accessKey + "/xml")
                .build();
    }

    @Override
    public NfcomStatusResult queryStatus(String accessKeyOrExternalId, FiscalGatewayConfig config) {
        return NfcomStatusResult.builder()
                .success(true)
                .chaveAcesso(accessKeyOrExternalId)
                .status("AUTORIZADA")
                .protocoloAutorizacao("115260001112223")
                .dataAutorizacao(LocalDateTime.now())
                .danfePdfUrl("https://pay.xingubit.com.br/v1/invoices/nfcom/" + accessKeyOrExternalId + "/pdf")
                .xmlUrl("https://pay.xingubit.com.br/v1/invoices/nfcom/" + accessKeyOrExternalId + "/xml")
                .build();
    }

    @Override
    public byte[] downloadDanfePdf(String accessKeyOrExternalId, FiscalGatewayConfig config) {
        return "%PDF-1.4\nMock DANFE\n%%EOF".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String downloadXml(String accessKeyOrExternalId, FiscalGatewayConfig config) {
        return "<xml>Mock XML</xml>";
    }

    @Override
    public NfcomCancelResult cancelNfcom(String accessKeyOrExternalId, String reason, FiscalGatewayConfig config) {
        return NfcomCancelResult.builder()
                .success(true)
                .chaveAcesso(accessKeyOrExternalId)
                .protocoloCancelamento("11526999000111")
                .dataCancelamento(LocalDateTime.now())
                .build();
    }
}
