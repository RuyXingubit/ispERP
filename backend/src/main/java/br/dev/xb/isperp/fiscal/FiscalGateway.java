package br.dev.xb.isperp.fiscal;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.fiscal.dto.*;

public interface FiscalGateway {

    /**
     * Identificador do tipo de gateway implementado por este driver.
     */
    FiscalGatewayType getGatewayType();

    /**
     * Cadastra ou atualiza os dados fiscais da empresa emissora no gateway fiscal.
     */
    boolean registerCompany(FiscalCompany company, FiscalGatewayConfig config);

    /**
     * Parametriza a série, lote e ambiente de NFCom (Modelo 62) para o CNPJ no gateway.
     */
    boolean configureNfcom(FiscalCompany company, FiscalGatewayConfig config);

    /**
     * Realiza o upload do certificado digital A1 (.pfx) para o cofre seguro do gateway.
     */
    CertificateUploadResult uploadCertificate(byte[] pfxBytes, String password, FiscalGatewayConfig config);

    /**
     * Emite uma NFCom (Modelo 62) no SEFAZ através do gateway fiscal.
     */
    NfcomIssueResult issueNfcom(NfcomIssueRequest request, FiscalCompany company, FiscalGatewayConfig config);

    /**
     * Consulta o status de processamento e protocolo de autorização de uma NFCom.
     */
    NfcomStatusResult queryStatus(String accessKeyOrExternalId, FiscalGatewayConfig config);

    /**
     * Baixa o binário do DANFE da NFCom em PDF.
     */
    byte[] downloadDanfePdf(String accessKeyOrExternalId, FiscalGatewayConfig config);

    /**
     * Baixa o XML protocolado e assinado pela SEFAZ.
     */
    String downloadXml(String accessKeyOrExternalId, FiscalGatewayConfig config);

    /**
     * Realiza o cancelamento de uma NFCom previamente autorizada.
     */
    NfcomCancelResult cancelNfcom(String accessKeyOrExternalId, String reason, FiscalGatewayConfig config);
}
