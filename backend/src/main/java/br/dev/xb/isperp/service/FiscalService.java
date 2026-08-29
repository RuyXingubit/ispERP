package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.fiscal.FiscalGateway;
import br.dev.xb.isperp.fiscal.FiscalGatewayResolver;
import br.dev.xb.isperp.fiscal.dto.*;
import br.dev.xb.isperp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class FiscalService {

    private final FiscalCompanyRepository companyRepository;
    private final FiscalGatewayConfigRepository configRepository;
    private final NfcomRecordRepository nfcomRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final PlanRepository planRepository;
    private final FiscalGatewayResolver gatewayResolver;

    /**
     * Cadastra ou atualiza os dados fiscais da empresa e propaga para o gateway fiscal.
     */
    @Transactional
    public FiscalCompany saveCompany(FiscalCompany company, @Nullable FiscalGatewayConfig customConfig) {
        FiscalCompany savedCompany = companyRepository.save(company);

        FiscalGatewayResolver.ResolvedFiscalGateway resolved = gatewayResolver.resolve(savedCompany.getId());
        FiscalGatewayConfig configToUse = (customConfig != null) ? customConfig : resolved.config();

        if (customConfig != null) {
            customConfig.setCompanyId(savedCompany.getId());
            configRepository.save(customConfig);
        }

        // Onboarding no Gateway
        try {
            resolved.gateway().registerCompany(savedCompany, configToUse);
            resolved.gateway().configureNfcom(savedCompany, configToUse);
        } catch (Exception e) {
            log.warn("Aviso ao propagar empresa ao gateway fiscal: {}", e.getMessage());
        }

        return savedCompany;
    }

    /**
     * Realiza o upload do Certificado Digital A1 (.pfx) para a empresa no gateway fiscal.
     */
    @Transactional
    public CertificateUploadResult uploadCertificate(UUID companyId, byte[] pfxBytes, String password) {
        FiscalCompany company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Empresa fiscal não encontrada"));

        FiscalGatewayResolver.ResolvedFiscalGateway resolved = gatewayResolver.resolve(companyId);
        CertificateUploadResult result = resolved.gateway().uploadCertificate(pfxBytes, password, resolved.config());

        if (result.isSuccess()) {
            company.setHasCertificate(true);
            company.setCertificateExpiresAt(result.getValidUntil() != null ? result.getValidUntil() : LocalDateTime.now().plusYears(1));
            companyRepository.save(company);
        }

        return result;
    }

    /**
     * Emite a NFCom (Modelo 62) para a fatura informada.
     */
    @Transactional
    public NfcomRecord issueNfcomForInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));

        Customer customer = customerRepository.findById(invoice.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente da fatura não encontrado"));

        Contract contract = invoice.getContractId() != null
                ? contractRepository.findById(invoice.getContractId()).orElse(null)
                : null;

        Plan plan = (contract != null && contract.getPlanId() != null)
                ? planRepository.findById(contract.getPlanId()).orElse(null)
                : null;

        FiscalGatewayResolver.ResolvedFiscalGateway resolved = gatewayResolver.resolve(null);
        FiscalCompany company = resolved.company();
        FiscalGateway gateway = resolved.gateway();
        FiscalGatewayConfig config = resolved.config();

        String planDesc = (plan != null)
                ? "Acesso Internet Banda Larga - " + plan.getName()
                : "Serviço de Comunicação Multimídia - Conexão Fibra Óptica";

        NfcomIssueRequest issueRequest = NfcomIssueRequest.builder()
                .invoiceId(invoice.getId())
                .contractId(contract != null ? contract.getId() : null)
                .customerId(customer.getId())
                .customerName(customer.getName())
                .customerDocument(customer.getCpf())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhone())
                .customerStreet(customer.getAddress() != null ? customer.getAddress() : "Logradouro Principal")
                .customerNumber("100")
                .customerNeighborhood("Centro")
                .customerCity(customer.getCity() != null ? customer.getCity() : "Altamira")
                .customerState(customer.getState() != null ? customer.getState() : "PA")
                .customerZipCode(customer.getZipCode() != null ? customer.getZipCode() : "68370000")
                .customerIbgeCode("1500602")
                .totalAmount(invoice.getAmount())
                .dueDate(invoice.getDueDate())
                .description(planDesc)
                .items(List.of(
                        NfcomIssueRequest.NfcomItemDTO.builder()
                                .description(planDesc)
                                .quantity(1)
                                .unitPrice(invoice.getAmount())
                                .totalPrice(invoice.getAmount())
                                .cnae(company.getCnaePrincipal() != null ? company.getCnaePrincipal().replaceAll("[^0-9]", "") : "6110803")
                                .cfop("5307")
                                .classificationCode("01.01.01")
                                .build()
                ))
                .build();

        NfcomIssueResult issueResult = gateway.issueNfcom(issueRequest, company, config);

        if (!issueResult.isSuccess()) {
            invoice.setNfcomStatus(Invoice.NfcomStatus.FAILED);
            invoice.setNfcomErrorMessage(issueResult.getErrorMessage());
            invoiceRepository.save(invoice);
            throw new RuntimeException("Falha ao emitir NFCom: " + issueResult.getErrorMessage());
        }

        // Incrementa sequencial da empresa
        company.setNfcomProximoNumero(company.getNfcomProximoNumero() + 1);
        companyRepository.save(company);

        // Salva Registro da NFCom
        NfcomRecord record = NfcomRecord.builder()
                .companyId(company.getId() != null ? company.getId() : UUID.randomUUID())
                .invoiceId(invoice.getId())
                .contractId(contract != null ? contract.getId() : null)
                .customerId(customer.getId())
                .chaveAcesso(issueResult.getChaveAcesso())
                .numero(issueResult.getNumero() != null ? issueResult.getNumero() : company.getNfcomProximoNumero() - 1)
                .serie(issueResult.getSerie() != null ? issueResult.getSerie() : company.getNfcomSerie())
                .modelo("62")
                .status(issueResult.getStatus() != null ? issueResult.getStatus() : "AUTORIZADA")
                .protocoloAutorizacao(issueResult.getProtocoloAutorizacao())
                .dataAutorizacao(issueResult.getDataAutorizacao() != null ? issueResult.getDataAutorizacao() : LocalDateTime.now())
                .digestValue(issueResult.getDigestValue())
                .valorTotal(invoice.getAmount())
                .valorIcms(BigDecimal.ZERO)
                .valorFust(invoice.getAmount().multiply(new BigDecimal("0.0065")))
                .valorFunttel(invoice.getAmount().multiply(new BigDecimal("0.0050")))
                .danfePdfUrl(issueResult.getDanfePdfUrl())
                .xmlAutorizado(issueResult.getXmlUrl())
                .build();

        NfcomRecord savedRecord = nfcomRecordRepository.save(record);

        // Atualiza a Fatura
        invoice.setNfcomNumber(savedRecord.getNumero());
        invoice.setNfcomSeries(Integer.parseInt(savedRecord.getSerie()));
        invoice.setNfcomKey(savedRecord.getChaveAcesso());
        invoice.setNfcomPdfUrl(savedRecord.getDanfePdfUrl());
        invoice.setNfcomXmlUrl(savedRecord.getXmlAutorizado());
        invoice.setNfcomStatus(Invoice.NfcomStatus.ISSUED);
        invoice.setNfcomIssuedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        log.info("NFCom nº {} (Chave {}) emitida com sucesso para a Fatura {}",
                savedRecord.getNumero(), savedRecord.getChaveAcesso(), invoice.getId());

        return savedRecord;
    }

    /**
     * Cancela uma NFCom emitida.
     */
    @Transactional
    public NfcomCancelResult cancelNfcom(UUID nfcomRecordId, String reason) {
        NfcomRecord record = nfcomRecordRepository.findById(nfcomRecordId)
                .orElseThrow(() -> new RuntimeException("Registro de NFCom não encontrado"));

        FiscalGatewayResolver.ResolvedFiscalGateway resolved = gatewayResolver.resolve(record.getCompanyId());
        NfcomCancelResult cancelResult = resolved.gateway().cancelNfcom(record.getChaveAcesso(), reason, resolved.config());

        if (cancelResult.isSuccess()) {
            record.setStatus("CANCELADA");
            record.setMotivoCancelamento(reason);
            record.setDataCancelamento(LocalDateTime.now());
            nfcomRecordRepository.save(record);

            if (record.getInvoiceId() != null) {
                invoiceRepository.findById(record.getInvoiceId()).ifPresent(inv -> {
                    inv.setNfcomStatus(Invoice.NfcomStatus.CANCELLED);
                    invoiceRepository.save(inv);
                });
            }
        }

        return cancelResult;
    }

    public Page<NfcomRecord> listRecords(Pageable pageable) {
        return nfcomRecordRepository.findAll(pageable);
    }
}
