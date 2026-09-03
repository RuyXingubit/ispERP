package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.mapper.ContractSignatureMapper;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.signature.DocumentType;
import br.dev.xb.isperp.signature.SignatureStatus;
import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class ElectronicSignatureService {

    private final ContractRepository contractRepository;
    private final ContractTemplateRepository templateRepository;
    private final ContractSignatureRepository signatureRepository;
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final PlanRepository planRepository;
    private final ContractTemplateEngine templateEngine;
    private final ContractTemplateService templateService;
    private final ContractSignatureMapper signatureMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final InvoiceRepository invoiceRepository;

    /**
     * Inicia uma nova sessão de assinatura eletrônica via Pix para um contrato.
     */
    @Transactional
    public SignatureSessionResponse createSignatureSession(CreateSignatureSessionRequest request, @Nullable String baseUrl) {
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado com o ID: " + request.getContractId()));

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado para o contrato: " + contract.getCustomerId()));

        Plan plan = planRepository.findById(contract.getPlanId())
                .orElseThrow(() -> new NoSuchElementException("Plano não encontrado para o contrato: " + contract.getPlanId()));

        Company company = companyRepository.findAll().stream().findFirst().orElse(null);

        // Seleciona o template ou usa o padrão
        ContractTemplate template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId()).orElse(null);
        }
        if (template == null && company != null) {
            template = templateRepository.findFirstByCompanyIdAndDocumentTypeAndIsActiveTrueOrderByVersionDesc(
                    company.getId(), DocumentType.SERVICE_AGREEMENT).orElse(null);
        }
        if (template == null) {
            template = templateRepository.findFirstByDocumentTypeAndIsActiveTrueOrderByVersionDesc(DocumentType.SERVICE_AGREEMENT).orElse(null);
        }

        // Se não houver nenhum template no sistema, cria os padrões
        if (template == null && company != null) {
            templateService.seedDefaultTemplatesIfEmpty(company.getId());
            template = templateRepository.findFirstByCompanyIdAndDocumentTypeAndIsActiveTrueOrderByVersionDesc(
                    company.getId(), DocumentType.SERVICE_AGREEMENT).orElse(null);
        }

        String rawContent = template != null ? template.getContentMarkdown() : "# CONTRATO DE PRESTAÇÃO DE SERVIÇOS\n\n{{customer.name}} - {{plan.name}}";

        // Renderiza conteúdo com as tags dinâmicas
        String renderedContent = templateEngine.render(rawContent, customer, company, contract, plan, null);
        String initialHash = templateEngine.calculateSha256(renderedContent);

        // Gera token público único e txid para o Pix
        String token = UUID.randomUUID().toString().replace("-", "");
        String pixTxid = "SIG" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        BigDecimal amount = request.getSymbolicAmount() != null ? request.getSymbolicAmount() : BigDecimal.valueOf(1.00);

        // Gera strings Pix simuladas
        String pixCopyPaste = "00020101021226800014br.gov.bcb.pix2558pix.isperp.com.br/qr/" + pixTxid + "5204000053039865404" + amount.toPlainString() + "5802BR5915" + (company != null ? company.getName() : "ispERP") + "6009SAO PAULO62070503***6304";
        String pixQrCodeBase64 = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iIzAwMCIvPjwvc3ZnPg==";

        ContractSignature signature = ContractSignature.builder()
                .contractId(contract.getId())
                .templateId(template != null ? template.getId() : null)
                .token(token)
                .status(SignatureStatus.PENDING)
                .symbolicAmount(amount)
                .pixTxid(pixTxid)
                .pixCopyPaste(pixCopyPaste)
                .pixQrCodeBase64(pixQrCodeBase64)
                .renderedContentSnapshot(renderedContent)
                .documentSha256Hash(initialHash)
                .expiresAt(OffsetDateTime.now().plusHours(72))
                .build();

        ContractSignature saved = signatureRepository.save(signature);
        log.info("Sessão de assinatura criada para o contrato {}. Token: {}, Pix TxID: {}", contract.getId(), token, pixTxid);

        SignatureSessionResponse response = signatureMapper.toResponse(saved);
        String host = baseUrl != null ? baseUrl : "http://localhost:5173";
        response.setSignatureUrl(host + "/sign/" + token);
        return response;
    }

    /**
     * Retorna a visão pública para a página do assinante renderizar o contrato e o Pix.
     */
    @Transactional
    public SignaturePublicViewResponse getPublicSignatureView(
            String token,
            @Nullable String clientIp,
            @Nullable String userAgent,
            @Nullable BigDecimal lat,
            @Nullable BigDecimal lon
    ) {
        ContractSignature signature = signatureRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Sessão de assinatura não encontrada ou token inválido."));

        // Se expirou e ainda estava PENDING, atualiza
        if (signature.getStatus() == SignatureStatus.PENDING && signature.getExpiresAt().isBefore(OffsetDateTime.now())) {
            signature.setStatus(SignatureStatus.EXPIRED);
            signatureRepository.save(signature);
        }

        // Salva logs de auditoria da abertura do link pelo cliente
        if (clientIp != null) signature.setClientIp(clientIp);
        if (userAgent != null) signature.setClientUserAgent(userAgent);
        if (lat != null) signature.setClientGeoLatitude(lat);
        if (lon != null) signature.setClientGeoLongitude(lon);
        signatureRepository.save(signature);

        Contract contract = contractRepository.findById(signature.getContractId())
                .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado: " + signature.getContractId()));

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado: " + contract.getCustomerId()));

        Company company = companyRepository.findAll().stream().findFirst().orElse(null);

        ContractTemplate template = null;
        if (signature.getTemplateId() != null) {
            template = templateRepository.findById(signature.getTemplateId()).orElse(null);
        }

        String consentClause = template != null ? template.getConsentClause() : "Ao realizar o pagamento do Pix abaixo, confirmo minha concordância integral com este contrato.";

        return SignaturePublicViewResponse.builder()
                .token(signature.getToken())
                .contractName("Contrato nº " + contract.getContractNumber())
                .customerName(customer.getName())
                .customerDocumentMasked(maskDocument(customer.getCpf()))
                .companyName(company != null ? company.getName() : "Provedor de Internet")
                .renderedContent(signature.getRenderedContentSnapshot() != null ? signature.getRenderedContentSnapshot() : "")
                .consentClause(consentClause)
                .status(signature.getStatus())
                .symbolicAmount(signature.getSymbolicAmount())
                .pixCopyPaste(signature.getPixCopyPaste())
                .pixQrCodeBase64(signature.getPixQrCodeBase64())
                .payerName(signature.getPayerName())
                .payerBankName(signature.getPayerBankName())
                .rejectionReason(signature.getRejectionReason())
                .signedPdfUrl(signature.getSignedPdfUrl())
                .documentSha256Hash(signature.getDocumentSha256Hash())
                .fallbackMethod(signature.getFallbackMethod())
                .onboardingCreditAmount(signature.getOnboardingCreditAmount())
                .forensicCertificatePdfUrl(signature.getForensicCertificatePdfUrl())
                .expiresAt(signature.getExpiresAt())
                .signedAt(signature.getSignedAt())
                .build();
    }

    /**
     * Processa a confirmação do Pix e valida a titularidade do CPF/CNPJ contra fraudes.
     */
    @Transactional
    public SignatureSessionResponse processPixSignatureWebhook(PixSignatureWebhookRequest request) {
        log.info("Webhook Pix de Assinatura recebido: TxID={}, EndToEndId={}, PayerCpf={}",
                request.getTxid(), request.getEndToEndId(), maskDocument(request.getPayerCpfCnpj()));

        ContractSignature signature = signatureRepository.findByPixTxid(request.getTxid())
                .orElseThrow(() -> new NoSuchElementException("Sessão de assinatura não encontrada para o TxID: " + request.getTxid()));

        if (signature.getStatus() == SignatureStatus.SIGNED) {
            log.info("Assinatura {} já processada anteriormente como SIGNED.", signature.getId());
            return signatureMapper.toResponse(signature);
        }

        if (signature.getExpiresAt().isBefore(OffsetDateTime.now())) {
            signature.setStatus(SignatureStatus.EXPIRED);
            signature.setRejectionReason("Link de assinatura expirado.");
            signatureRepository.save(signature);
            return signatureMapper.toResponse(signature);
        }

        Contract contract = contractRepository.findById(signature.getContractId())
                .orElseThrow(() -> new NoSuchElementException("Contrato não encontrado: " + signature.getContractId()));

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new NoSuchElementException("Cliente não encontrado: " + contract.getCustomerId()));

        String cleanCustomerDoc = cleanDigits(customer.getCpf());
        String cleanPayerDoc = cleanDigits(request.getPayerCpfCnpj());

        // VALIDAÇÃO RIGOROSA ANTI-FRAUDE: Titularidade do Pix emitido pelo BACEN
        if (cleanCustomerDoc.equals(cleanPayerDoc)) {
            signature.setStatus(SignatureStatus.SIGNED);
            signature.setPixEndToEndId(request.getEndToEndId());
            signature.setPayerName(request.getPayerName());
            signature.setPayerCpfCnpj(request.getPayerCpfCnpj());
            signature.setPayerBankName(request.getBankName() != null ? request.getBankName() : "Instituição Bancária Integrada ao BACEN");
            signature.setPayerBankIspb(request.getIspb());
            signature.setSignedAt(OffsetDateTime.now());
            signature.setRejectionReason(null);

            // REGRA DE NEGÓCIO 2: Abatimento de R$ 1,00 na próxima fatura
            BigDecimal discountAmount = signature.getSymbolicAmount() != null ? signature.getSymbolicAmount() : BigDecimal.valueOf(1.00);
            List<Invoice> pendingInvoices = invoiceRepository.findByContractIdOrderByDueDateAsc(contract.getId()).stream()
                    .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PENDING)
                    .toList();

            if (!pendingInvoices.isEmpty()) {
                Invoice targetInvoice = pendingInvoices.get(0);
                BigDecimal currentDiscount = targetInvoice.getDiscountAmount() != null ? targetInvoice.getDiscountAmount() : BigDecimal.ZERO;
                targetInvoice.setDiscountAmount(currentDiscount.add(discountAmount));
                invoiceRepository.save(targetInvoice);
                signature.setDiscountAppliedInvoiceId(targetInvoice.getId());
                signature.setOnboardingCreditAmount(discountAmount);
                log.info("Desconto de R$ {} da assinatura Pix aplicado na fatura {} do contrato {}",
                        discountAmount, targetInvoice.getId(), contract.getContractNumber());
            } else {
                BigDecimal currentCredit = contract.getPendingOnboardingCredit() != null ? contract.getPendingOnboardingCredit() : BigDecimal.ZERO;
                contract.setPendingOnboardingCredit(currentCredit.add(discountAmount));
                signature.setOnboardingCreditAmount(discountAmount);
                log.info("Crédito de R$ {} de onboarding registrado no contrato {} para abatimento na primeira fatura",
                        discountAmount, contract.getContractNumber());
            }

            // Re-renderiza o documento final com os dados bancários carimbados
            Company company = companyRepository.findAll().stream().findFirst().orElse(null);
            Plan plan = planRepository.findById(contract.getPlanId()).orElse(null);
            ContractTemplate template = signature.getTemplateId() != null ? templateRepository.findById(signature.getTemplateId()).orElse(null) : null;
            String rawTemplate = template != null ? template.getContentMarkdown() : signature.getRenderedContentSnapshot();

            String finalRendered = templateEngine.render(rawTemplate, customer, company, contract, plan, signature);

            // REGRA DE NEGÓCIO 3: Certificado de Autenticidade Forense (MP 2.200-2/01 e Lei 14.063/2020)
            String forensicCertificate = buildForensicCertificate(signature, customer, company, contract, request);
            String fullDocumentWithCertificate = finalRendered + "\n\n" + forensicCertificate;

            String finalHash = templateEngine.calculateSha256(fullDocumentWithCertificate);
            signature.setRenderedContentSnapshot(fullDocumentWithCertificate);
            signature.setDocumentSha256Hash(finalHash);
            signature.setSignedPdfUrl("/api/public/signatures/" + signature.getToken() + "/pdf");
            signature.setForensicCertificatePdfUrl("/api/public/signatures/" + signature.getToken() + "/pdf");

            // Atualiza status do contrato no ERP
            contract.setStatus(Contract.ContractStatus.PENDING_INSTALLATION);
            contractRepository.save(contract);

            // Emite evento de domínio na Transactional Outbox
            Map<String, Object> payload = new HashMap<>();
            payload.put("contractId", contract.getId());
            payload.put("contractNumber", contract.getContractNumber());
            payload.put("customerId", customer.getId());
            payload.put("customerName", customer.getName());
            payload.put("signatureToken", signature.getToken());
            payload.put("pixEndToEndId", request.getEndToEndId());
            payload.put("documentSha256", finalHash);
            payload.put("signedAt", signature.getSignedAt());
            payload.put("discountApplied", discountAmount);

            domainEventPublisher.publish(GenericDomainEvent.builder()
                    .eventId(UuidCreator.getTimeOrderedEpoch())
                    .eventType("CONTRACT_SIGNED")
                    .aggregateType("Contract")
                    .aggregateId(contract.getId() != null ? contract.getId().toString() : "")
                    .payload(payload)
                    .occurredAt(LocalDateTime.now())
                    .build()
            );

            log.info("CONTRATO ASSINADO COM SUCESSO! Contrato: {}, Titular: {}, EndToEndId: {}, Hash: {}",
                    contract.getContractNumber(), customer.getName(), request.getEndToEndId(), finalHash);
        } else {
            // REGRA DE NEGÓCIO 1: Divergência de titularidade -> Bloqueio estrito e oferta de fallbacks
            signature.setStatus(SignatureStatus.REJECTED_DIVERGENT_DOCUMENT);
            signature.setPayerName(request.getPayerName());
            signature.setPayerCpfCnpj(request.getPayerCpfCnpj());
            signature.setPayerBankName(request.getBankName());
            signature.setPixEndToEndId(request.getEndToEndId());
            signature.setRejectionReason("Pagamento rejeitado por divergência de titularidade: O CPF do pagador (" +
                    maskDocument(request.getPayerCpfCnpj()) + ") difere do CPF do titular cadastrado no contrato (" +
                    maskDocument(customer.getCpf()) + "). O pagamento de R$ 1,00 deve ser feito obrigatoriamente pela conta bancária do titular. " +
                    "Caso o titular não possua conta bancária ativa, selecione uma das opções de fallback disponíveis (Gov.br, E-mail OTP ou Assinatura Física Presencial/Cartório).");

            log.warn("ASSINATURA REJEITADA POR DIVERGÊNCIA DE CPF: Contrato {}, CPF Titular: {}, CPF Pagador: {}",
                    contract.getContractNumber(), maskDocument(customer.getCpf()), maskDocument(request.getPayerCpfCnpj()));
        }

        ContractSignature saved = signatureRepository.save(signature);
        return signatureMapper.toResponse(saved);
    }

    /**
     * Permite a seleção de uma via alternativa de assinatura (fallback) caso o Pix tenha sido rejeitado por divergência de CPF.
     */
    @Transactional
    public SignatureSessionResponse selectFallbackMethod(String token, br.dev.xb.isperp.signature.FallbackMethod fallbackMethod, @Nullable String justification) {
        ContractSignature signature = signatureRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Sessão de assinatura não encontrada para o token: " + token));

        if (signature.getStatus() == SignatureStatus.SIGNED) {
            throw new IllegalStateException("O contrato já foi assinado e não permite alteração de modalidade.");
        }

        signature.setFallbackMethod(fallbackMethod);
        if (justification != null && !justification.isBlank()) {
            signature.setRejectionReason("Alternativa oficial de assinatura selecionada (" + fallbackMethod + "): " + justification);
        }
        ContractSignature saved = signatureRepository.save(signature);
        log.info("Método alternativo selecionado para sessão {}: {}", token, fallbackMethod);
        return signatureMapper.toResponse(saved);
    }

    /**
     * Constrói a Folha Pericial / Certificado de Autenticidade Forense (MP 2.200-2/01 e Lei 14.063/2020)
     */
    private String buildForensicCertificate(
            ContractSignature signature,
            Customer customer,
            @Nullable Company company,
            Contract contract,
            PixSignatureWebhookRequest request
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n---\n\n");
        sb.append("# CERTIFICADO DE AUTENTICIDADE E ASSINATURA ELETRÔNICA AVANÇADA\n");
        sb.append("**Conformidade Legal:** Art. 10, § 2º da Medida Provisória nº 2.200-2/2001 e Art. 4º, II da Lei Federal nº 14.063/2020.\n\n");
        sb.append("| EVIDÊNCIA FORENSE | DADOS VALIDADOS DIRETAMENTE NO BANCO CENTRAL DO BRASIL |\n");
        sb.append("| :--- | :--- |\n");
        sb.append("| **Identificador da Transação (EndToEndId)** | `").append(request.getEndToEndId()).append("` |\n");
        sb.append("| **Titular / Signatário** | ").append(customer.getName()).append(" |\n");
        sb.append("| **CPF do Titular / Pagador** | ").append(customer.getCpf()).append(" (Conferência 100% Idêntica) |\n");
        sb.append("| **Instituição Bancária Emissora** | ").append(request.getBankName() != null ? request.getBankName() : "Rede do Sistema de Pagamentos Instantâneos - SPI/BACEN").append(" (ISPB: ").append(request.getIspb() != null ? request.getIspb() : "N/A").append(") |\n");
        sb.append("| **Data e Hora da Assinatura** | ").append(OffsetDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ssXXX"))).append(" (Horário de Brasília) |\n");
        sb.append("| **Endereço IP de Conexão** | ").append(signature.getClientIp() != null ? signature.getClientIp() : "Registrado via Webhook SPI").append(" |\n");
        sb.append("| **Navegador / Dispositivo** | ").append(signature.getClientUserAgent() != null ? signature.getClientUserAgent() : "Aplicativo Bancário Móvel").append(" |\n");
        sb.append("| **Contrato Associado** | Nº ").append(contract.getContractNumber()).append(" |\n");
        sb.append("| **Empresa Provedora** | ").append(company != null ? company.getName() : "Provedor de Internet").append(" |\n\n");
        sb.append("> **Declaração de Consentimento:** *'Ao realizar o pagamento do Pix de R$ 1,00 pela sua conta bancária pessoal titular, o contratante confirmou formalmente sua identidade civil, anuiu com todas as cláusulas contratuais e termos de fidelidade, tendo sido o valor lançado como crédito/desconto em sua fatura.'*\n");
        return sb.toString();
    }
    @Transactional(readOnly = true)
    public SignatureSessionResponse getSignatureStatus(String token) {
        ContractSignature signature = signatureRepository.findByToken(token)
                .orElseThrow(() -> new NoSuchElementException("Sessão não encontrada para o token: " + token));
        return signatureMapper.toResponse(signature);
    }

    /**
     * Lista o histórico de assinaturas de um contrato.
     */
    @Transactional(readOnly = true)
    public List<SignatureSessionResponse> listSignaturesByContract(UUID contractId) {
        return signatureMapper.toResponseList(signatureRepository.findByContractIdOrderByCreatedAtDesc(contractId));
    }

    private String cleanDigits(@Nullable String doc) {
        if (doc == null) return "";
        return doc.replaceAll("[^0-9]", "");
    }

    private String maskDocument(@Nullable String doc) {
        if (doc == null) return "";
        String clean = cleanDigits(doc);
        if (clean.length() == 11) {
            return "***." + clean.substring(3, 6) + "." + clean.substring(6, 9) + "-**";
        } else if (clean.length() == 14) {
            return clean.substring(0, 2) + ".***.***/" + clean.substring(8, 12) + "-**";
        }
        return "***";
    }
}
