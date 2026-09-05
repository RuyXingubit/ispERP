package br.dev.xb.isperp.fiscal.xingubit;

import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.FiscalGatewayConfig;
import br.dev.xb.isperp.fiscal.FiscalGateway;
import br.dev.xb.isperp.fiscal.FiscalGatewayType;
import br.dev.xb.isperp.fiscal.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@SuppressWarnings("null")
public class XingubitPayFiscalDriver implements FiscalGateway {

    private final RestClient.Builder restClientBuilder;

    public XingubitPayFiscalDriver(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder != null ? restClientBuilder : RestClient.builder();
    }

    public XingubitPayFiscalDriver() {
        this(RestClient.builder());
    }

    @Override
    public FiscalGatewayType getGatewayType() {
        return FiscalGatewayType.XINGUBIT_PAY;
    }

    private String getAuthToken(FiscalGatewayConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";
        String clientId = config.getClientId() != null ? config.getClientId() : "isperp_client";
        String clientSecret = config.getClientSecret() != null ? config.getClientSecret() : "isperp_secret";

        try {
            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                    .uri("/v1/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("client_id=" + clientId + "&client_secret=" + clientSecret)
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse != null && tokenResponse.containsKey("access_token")) {
                return (String) tokenResponse.get("access_token");
            }
            throw new IllegalStateException("Resposta de autenticação do Xingubit Pay não contém access_token válido.");
        } catch (Exception e) {
            log.error("Falha na autenticação OAuth2 com Xingubit Pay: {}", e.getMessage());
            throw new IllegalStateException("Não foi possível autenticar junto ao gateway fiscal Xingubit Pay: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean registerCompany(FiscalCompany company, FiscalGatewayConfig config) {
        log.info("Cadastrando/Atualizando empresa emissora CNPJ {} no Xingubit Pay", company.getCnpj());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";

        Map<String, Object> payload = new HashMap<>();
        payload.put("cnpj", company.getCnpj());
        payload.put("razaoSocial", company.getRazaoSocial());
        payload.put("nomeFantasia", company.getNomeFantasia());
        payload.put("inscricaoEstadual", company.getInscricaoEstadual());
        payload.put("inscricaoMunicipal", company.getInscricaoMunicipal());
        payload.put("cnaePrincipal", company.getCnaePrincipal());
        payload.put("regimeTributario", company.getRegimeTributario());
        payload.put("logradouro", company.getLogradouro());
        payload.put("numero", company.getNumero());
        payload.put("complemento", company.getComplemento());
        payload.put("bairro", company.getBairro());
        payload.put("cidade", company.getCidade());
        payload.put("uf", company.getUf());
        payload.put("cep", company.getCep());
        payload.put("codigoIbge", company.getCodigoIbge());
        payload.put("telefone", company.getTelefone());
        payload.put("emailFiscal", company.getEmailFiscal());

        try {
            String token = getAuthToken(config);
            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            restClient.post()
                    .uri("/v1/empresas")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Falha na chamada REST /v1/empresas ao Xingubit Pay: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean configureNfcom(FiscalCompany company, FiscalGatewayConfig config) {
        log.info("Configurando parâmetros de NFCom para o CNPJ {} no Xingubit Pay", company.getCnpj());
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";
        String cleanCnpj = company.getCnpj().replaceAll("[^0-9]", "");

        Map<String, Object> payload = Map.of(
                "ambiente", company.getNfcomAmbiente().toLowerCase(),
                "serie", company.getNfcomSerie(),
                "proximoNumero", company.getNfcomProximoNumero()
        );

        try {
            String token = getAuthToken(config);
            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            restClient.put()
                    .uri("/v1/empresas/{cnpj}/config/nfcom", cleanCnpj)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            log.error("Falha na chamada REST /v1/empresas/{}/config/nfcom: {}", cleanCnpj, e.getMessage());
            return false;
        }
    }

    @Override
    public CertificateUploadResult uploadCertificate(byte[] pfxBytes, String password, FiscalGatewayConfig config) {
        log.info("Realizando upload de Certificado Digital A1 ({}) bytes para o Xingubit Pay", pfxBytes.length);
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";

        try {
            String token = getAuthToken(config);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(pfxBytes) {
                @Override
                public String getFilename() {
                    return "certificado_a1.pfx";
                }
            };
            body.add("file", new HttpEntity<>(resource));
            body.add("password", password);

            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1/merchants/me/certificate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new IllegalStateException("Resposta vazia do gateway ao enviar certificado.");
            }

            return CertificateUploadResult.builder()
                    .success(true)
                    .subjectCnpj((String) response.get("cnpj"))
                    .subjectName((String) response.get("razaoSocial"))
                    .validUntil(response.containsKey("validUntil") ? LocalDateTime.parse((String) response.get("validUntil")) : null)
                    .message("Certificado digital A1 configurado com sucesso no cofre seguro.")
                    .build();
        } catch (Exception e) {
            log.error("Upload de certificado no Xingubit Pay falhou: {}", e.getMessage());
            return CertificateUploadResult.builder()
                    .success(false)
                    .message("Falha ao registrar certificado digital: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public NfcomIssueResult issueNfcom(NfcomIssueRequest request, FiscalCompany company, FiscalGatewayConfig config) {
        log.info("Emitindo NFCom (Modelo 62) para o cliente {} (R$ {}) via Xingubit Pay",
                request.getCustomerName(), request.getTotalAmount());

        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";
        int nextNum = company.getNfcomProximoNumero();

        Map<String, Object> payload = new HashMap<>();
        payload.put("docType", "nfcom");
        payload.put("companyCnpj", company.getCnpj());
        payload.put("invoiceId", request.getInvoiceId() != null ? request.getInvoiceId().toString() : null);
        payload.put("customer", Map.of(
                "name", request.getCustomerName(),
                "document", request.getCustomerDocument(),
                "email", request.getCustomerEmail() != null ? request.getCustomerEmail() : "",
                "address", Map.of(
                        "street", request.getCustomerStreet(),
                        "number", request.getCustomerNumber(),
                        "neighborhood", request.getCustomerNeighborhood(),
                        "city", request.getCustomerCity(),
                        "state", request.getCustomerState(),
                        "zipCode", request.getCustomerZipCode(),
                        "ibgeCode", request.getCustomerIbgeCode() != null ? request.getCustomerIbgeCode() : "1500602"
                )
        ));
        payload.put("totalAmount", request.getTotalAmount());
        payload.put("items", request.getItems() != null ? request.getItems() : List.of(
                Map.of(
                        "description", "Serviço de Comunicação Multimídia - Internet Banda Larga",
                        "quantity", 1,
                        "unitPrice", request.getTotalAmount(),
                        "totalPrice", request.getTotalAmount(),
                        "cfop", "5307",
                        "cnae", "6110803"
                )
        ));

        try {
            String token = getAuthToken(config);
            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1/invoices/nfcom")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            if (response != null && "AUTORIZADA".equalsIgnoreCase((String) response.get("status"))) {
                return NfcomIssueResult.builder()
                        .success(true)
                        .externalId((String) response.getOrDefault("id", "XINGUBIT-NFCOM-" + nextNum))
                        .chaveAcesso((String) response.get("chaveAcesso"))
                        .numero(nextNum)
                        .serie(company.getNfcomSerie())
                        .status("AUTORIZADA")
                        .protocoloAutorizacao((String) response.get("protocoloAutorizacao"))
                        .dataAutorizacao(LocalDateTime.now())
                        .digestValue((String) response.get("digestValue"))
                        .danfePdfUrl((String) response.get("danfePdfUrl"))
                        .xmlUrl((String) response.get("xmlUrl"))
                        .build();
            } else {
                String errorReason = response != null && response.containsKey("motivoRejeicao")
                        ? (String) response.get("motivoRejeicao")
                        : "Emissão de NFCom rejeitada ou pendente de retorno da SEFAZ";
                return NfcomIssueResult.builder()
                        .success(false)
                        .errorMessage(errorReason)
                        .build();
            }
        } catch (Exception e) {
            log.error("Erro na comunicação com o gateway Xingubit Pay para emissão de NFCom: {}", e.getMessage());
            return NfcomIssueResult.builder()
                    .success(false)
                    .errorMessage("Falha de conexão com a API fiscal remota: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public NfcomStatusResult queryStatus(String accessKeyOrExternalId, FiscalGatewayConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";
        try {
            String token = getAuthToken(config);
            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("/v1/invoices/nfcom/{id}", accessKeyOrExternalId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response != null) {
                return NfcomStatusResult.builder()
                        .success(true)
                        .chaveAcesso((String) response.get("chaveAcesso"))
                        .status((String) response.get("status"))
                        .protocoloAutorizacao((String) response.get("protocoloAutorizacao"))
                        .dataAutorizacao(LocalDateTime.now())
                        .danfePdfUrl((String) response.get("danfePdfUrl"))
                        .xmlUrl((String) response.get("xmlUrl"))
                        .build();
            }
        } catch (Exception e) {
            log.error("Erro ao consultar status da NFCom no Xingubit Pay: {}", e.getMessage());
        }
        return NfcomStatusResult.builder()
                .success(false)
                .status("ERRO_CONSULTA")
                .errorMessage("Não foi possível consultar o status da nota fiscal no gateway")
                .build();
    }

    @Override
    public byte[] downloadDanfePdf(String accessKeyOrExternalId, FiscalGatewayConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";
        String token = getAuthToken(config);
        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
        return restClient.get()
                .uri("/v1/invoices/nfcom/{key}/pdf", accessKeyOrExternalId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(byte[].class);
    }

    @Override
    public String downloadXml(String accessKeyOrExternalId, FiscalGatewayConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";
        String token = getAuthToken(config);
        RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
        return restClient.get()
                .uri("/v1/invoices/nfcom/{key}/xml", accessKeyOrExternalId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(String.class);
    }

    @Override
    public NfcomCancelResult cancelNfcom(String accessKeyOrExternalId, String reason, FiscalGatewayConfig config) {
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "https://pay.xingubit.com.br";
        try {
            String token = getAuthToken(config);
            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/v1/invoices/nfcom/{id}/cancel", accessKeyOrExternalId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("justificativa", reason != null ? reason : "Cancelamento a pedido"))
                    .retrieve()
                    .body(Map.class);

            if (response != null && "CANCELADA".equalsIgnoreCase((String) response.get("status"))) {
                return NfcomCancelResult.builder()
                        .success(true)
                        .chaveAcesso(accessKeyOrExternalId)
                        .protocoloCancelamento((String) response.get("protocoloCancelamento"))
                        .dataCancelamento(LocalDateTime.now())
                        .build();
            }
        } catch (Exception e) {
            log.error("Erro ao cancelar NFCom no Xingubit Pay: {}", e.getMessage());
        }
        return NfcomCancelResult.builder()
                .success(false)
                .chaveAcesso(accessKeyOrExternalId)
                .errorMessage("Não foi possível cancelar a NFCom no gateway fiscal")
                .build();
    }

    @SuppressWarnings("unused")
    private int calculateMod11(String key) {
        int[] weights = {2, 3, 4, 5, 6, 7, 8, 9};
        int sum = 0;
        int weightIndex = 0;
        for (int i = key.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(key.charAt(i));
            sum += digit * weights[weightIndex];
            weightIndex = (weightIndex + 1) % weights.length;
        }
        int remainder = sum % 11;
        int result = 11 - remainder;
        return (result == 0 || result >= 10) ? 0 : result;
    }
}
