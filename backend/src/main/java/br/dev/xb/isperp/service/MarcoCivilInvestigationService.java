package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.MarcoCivilMapper;
import br.dev.xb.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarcoCivilInvestigationService {

    private final RadAcctRepository radAcctRepository;
    private final OnuProvisioningRepository onuProvisioningRepository;
    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final CgnatParserService cgnatParserService;
    private final MarcoCivilReportRepository marcoCivilReportRepository;
    private final MarcoCivilMapper marcoCivilMapper;

    @Value("${app.public-base-url:http://localhost:5173}")
    private String publicBaseUrl;

    /**
     * Executa a busca forense reversa para identificar o assinante responsável pelo IP no instante do fato.
     */
    @Transactional(readOnly = true)
    public MarcoCivilSearchResult searchSubscriber(MarcoCivilSearchRequest request) {
        log.info("Executando investigação Marco Civil: IP={}, Porta={}, Timestamp={}",
                request.getIp(), request.getPort(), request.getTimestamp());

        String targetSearchIp = request.getIp().trim();
        boolean usedCgnat = false;
        String resolvedPrivateIp = null;
        String cgnatRuleSummary = null;

        // 1. Se foi informada a porta lógica de origem, tenta decodificar via CGNAT
        if (request.getPort() != null && request.getPort() > 0) {
            Optional<String> cgnatMatch = cgnatParserService.findPrivateIpForPublicPort(targetSearchIp, request.getPort());
            if (cgnatMatch.isPresent()) {
                resolvedPrivateIp = cgnatMatch.get();
                usedCgnat = true;
                cgnatRuleSummary = String.format("Mapeamento CGNAT: IP Público %s Porta %d -> IP Privado %s",
                        targetSearchIp, request.getPort(), resolvedPrivateIp);
                targetSearchIp = resolvedPrivateIp;
            }
        }

        // 2. Busca na tabela de accounting do FreeRADIUS (radacct)
        String ipPrefixLike = targetSearchIp + "%";
        List<RadAcct> matchedSessions = radAcctRepository.findSessionByIpAndTimestamp(
                targetSearchIp,
                ipPrefixLike,
                request.getTimestamp()
        );

        if (matchedSessions.isEmpty()) {
            return MarcoCivilSearchResult.builder()
                    .matched(false)
                    .queriedIp(request.getIp())
                    .queriedPort(request.getPort())
                    .queriedTimestamp(request.getTimestamp())
                    .usedCgnat(usedCgnat)
                    .resolvedPrivateIp(resolvedPrivateIp)
                    .cgnatRuleSummary(cgnatRuleSummary)
                    .build();
        }

        // 3. Obtém a sessão mais relevante e cruza com o cadastro do assinante
        RadAcct session = matchedSessions.get(0);
        MarcoCivilSearchResult.MarcoCivilSearchResultBuilder resultBuilder = MarcoCivilSearchResult.builder()
                .matched(true)
                .queriedIp(request.getIp())
                .queriedPort(request.getPort())
                .queriedTimestamp(request.getTimestamp())
                .usedCgnat(usedCgnat)
                .resolvedPrivateIp(resolvedPrivateIp)
                .cgnatRuleSummary(cgnatRuleSummary)
                .radacctId(session.getRadacctId())
                .username(session.getUsername())
                .callingStationId(session.getCallingStationId())
                .nasIpAddress(session.getNasIpAddress())
                .sessionStartTime(session.getAcctStartTime())
                .sessionStopTime(session.getAcctStopTime());

        if (session.getUsername() != null) {
            onuProvisioningRepository.findByPppoeUser(session.getUsername())
                    .ifPresent(onu -> {
                        contractRepository.findById(onu.getContractId())
                                .ifPresent(contract -> {
                                    resultBuilder.contractId(contract.getId())
                                            .contractNumber(contract.getContractNumber());

                                    if (contract.getPlanId() != null) {
                                        planRepository.findById(contract.getPlanId())
                                                .ifPresent(plan -> resultBuilder.planName(plan.getName()));
                                    }
                                });

                        customerRepository.findById(onu.getCustomerId())
                                .ifPresent(customer -> {
                                    resultBuilder.customerId(customer.getId())
                                            .customerName(customer.getName())
                                            .customerCpfCnpj(customer.getCpf())
                                            .customerPhone(customer.getPhone())
                                            .customerEmail(customer.getEmail());

                                    if (customer.getAddress() != null) {
                                        String fullAddress = String.format("%s, %s - %s, CEP %s",
                                                customer.getAddress(),
                                                customer.getCity() != null ? customer.getCity() : "",
                                                customer.getState() != null ? customer.getState() : "",
                                                customer.getZipCode() != null ? customer.getZipCode() : "");
                                        resultBuilder.installationAddress(fullAddress);
                                    }
                                });
                    });
        }

        return resultBuilder.build();
    }

    /**
     * Emite o laudo pericial oficial com token de validação pública e assinatura criptográfica SHA-256.
     */
    @Transactional
    public MarcoCivilReportResponse generateOfficialReport(MarcoCivilReportRequest request) {
        log.info("Emitindo Laudo Pericial Oficial Marco Civil para IP: {}", request.getQueriedIp());

        // 1. Executa a investigação para capturar os fatos
        MarcoCivilSearchResult search = searchSubscriber(MarcoCivilSearchRequest.builder()
                .ip(request.getQueriedIp())
                .port(request.getQueriedPort())
                .timestamp(request.getQueriedTimestamp())
                .build());

        // 2. Gera Token de Validação Seguro
        String validationToken = UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.currentTimeMillis());

        // 3. Calcula Hash SHA-256 dos fatos periciais (Garante Imutabilidade Anti-Fraude)
        String canonicalData = String.format(
                "TOKEN:%s|IP:%s|PORT:%s|TIME:%s|USER:%s|CPF:%s|MAC:%s|OFICIO:%s",
                validationToken,
                request.getQueriedIp(),
                request.getQueriedPort() != null ? request.getQueriedPort() : "N/A",
                request.getQueriedTimestamp().toString(),
                search.getUsername() != null ? search.getUsername() : "UNKNOWN",
                search.getCustomerCpfCnpj() != null ? search.getCustomerCpfCnpj() : "UNKNOWN",
                search.getCallingStationId() != null ? search.getCallingStationId() : "UNKNOWN",
                request.getCourtOrderNumber() != null ? request.getCourtOrderNumber() : "N/A"
        );
        String sha256Hash = computeSha256(canonicalData);

        // 4. Salva a emissão do laudo no banco de dados
        MarcoCivilReport entity = marcoCivilMapper.toEntity(request);
        entity.setValidationToken(validationToken);
        entity.setSha256Hash(sha256Hash);
        entity.setMatchedContractId(search.getContractId());
        entity.setMatchedCustomerName(search.getCustomerName());
        entity.setMatchedCpfCnpj(search.getCustomerCpfCnpj());
        entity.setMatchedCallingStationId(search.getCallingStationId());
        entity.setMatchedSessionStart(search.getSessionStartTime());
        entity.setMatchedSessionStop(search.getSessionStopTime());

        MarcoCivilReport saved = marcoCivilReportRepository.save(entity);

        // 5. Monta links de validação e payload do QR Code
        String validationUrl = publicBaseUrl + "/public/validar-laudo/" + validationToken;

        MarcoCivilReportResponse response = marcoCivilMapper.toResponse(saved);
        response.setPublicValidationUrl(validationUrl);
        response.setQrCodePayload(validationUrl);

        return response;
    }

    /**
     * Validação pública de laudos periciais para autoridades policiais e judiciais
     */
    @Transactional(readOnly = true)
    public PublicValidationResponse validatePublicToken(String token) {
        return marcoCivilReportRepository.findByValidationToken(token)
                .map(report -> PublicValidationResponse.builder()
                        .valid(true)
                        .validationToken(report.getValidationToken())
                        .sha256Hash(report.getSha256Hash())
                        .courtOrderNumber(report.getCourtOrderNumber())
                        .requesterAuthority(report.getRequesterAuthority())
                        .queriedIp(report.getQueriedIp())
                        .queriedPort(report.getQueriedPort())
                        .queriedTimestamp(report.getQueriedTimestamp())
                        .customerNameMasked(maskName(report.getMatchedCustomerName()))
                        .customerCpfCnpjMasked(maskCpfCnpj(report.getMatchedCpfCnpj()))
                        .callingStationId(report.getMatchedCallingStationId())
                        .reportIssuedAt(report.getCreatedAt())
                        .statusMessage("DOCUMENTO AUTÊNTICO - Registro conferido e homologado via ispERP Marco Civil.")
                        .build())
                .orElseGet(() -> PublicValidationResponse.builder()
                        .valid(false)
                        .validationToken(token)
                        .sha256Hash("N/A")
                        .queriedIp("N/A")
                        .queriedTimestamp(OffsetDateTime.now())
                        .reportIssuedAt(OffsetDateTime.now())
                        .statusMessage("DOCUMENTO INVÁLIDO OU NÃO ENCONTRADO - Risco de fraude ou adulteração.")
                        .build());
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 não disponível", e);
        }
    }

    private String maskName(@org.jspecify.annotations.Nullable String name) {
        if (name == null || name.length() < 4) return "***";
        String[] parts = name.split(" ");
        if (parts.length == 1) return parts[0].substring(0, 2) + "***";
        return parts[0] + " *** " + parts[parts.length - 1];
    }

    private String maskCpfCnpj(@org.jspecify.annotations.Nullable String doc) {
        if (doc == null || doc.length() < 6) return "***";
        return doc.substring(0, 3) + ".***.***-" + doc.substring(doc.length() - 2);
    }
}
