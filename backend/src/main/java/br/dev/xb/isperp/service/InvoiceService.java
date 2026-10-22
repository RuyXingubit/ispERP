package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.InvoiceCompositionItem;
import br.dev.xb.isperp.entity.PaymentTransaction;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.PlanCompositionCategory;
import br.dev.xb.isperp.entity.PlanCompositionItem;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.gateway.PaymentGateway;
import br.dev.xb.isperp.gateway.PaymentGatewayResolver;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.gateway.dto.CreateChargeRequest;
import br.dev.xb.isperp.gateway.dto.CreateChargeResponse;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.PaymentTransactionRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentGatewayResolver gatewayResolver;
    private final DomainEventPublisher domainEventPublisher;
    private final AuditLogService auditLogService;

    @Nullable
    private PlanRepository planRepository;

    @Nullable
    private ContractRepository contractRepository;

    @Nullable
    private BillingNotificationDunningService billingNotificationDunningService;

    @Autowired(required = false)
    public void setPlanRepository(@Nullable PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Autowired(required = false)
    public void setContractRepository(@Nullable ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Autowired(required = false)
    public void setBillingNotificationDunningService(@Nullable BillingNotificationDunningService billingNotificationDunningService) {
        this.billingNotificationDunningService = billingNotificationDunningService;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> getInvoicesByCustomerId(UUID customerId) {
        return invoiceRepository.findByCustomerIdOrderByDueDateDesc(customerId);
    }

    public List<Invoice> getInvoicesByContractId(UUID contractId) {
        return invoiceRepository.findByContractIdOrderByDueDateDesc(contractId);
    }

    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatusOrderByDueDateAsc(status);
    }

    public Optional<Invoice> getInvoiceById(UUID id) {
        return invoiceRepository.findById(id);
    }

    public Optional<Invoice> getInvoiceByExternalTransactionId(String txId) {
        return invoiceRepository.findByExternalTransactionId(txId);
    }

    @Transactional
    public Invoice createInvoiceForContract(Contract contract, LocalDate dueDate) {
        log.info("Gerando fatura para contrato {}: vencimento={}, valor={}",
                contract.getContractNumber(), dueDate, contract.getMonthlyFee());

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente do contrato não encontrado"));

        UUID invoiceId = UuidCreatorUtils.generateUuidV7();

        BigDecimal proRataDelta = contract.getPendingProRataAdjustment() != null
                ? contract.getPendingProRataAdjustment()
                : BigDecimal.ZERO;
        BigDecimal effectiveAmount = contract.getMonthlyFee().add(proRataDelta);
        if (effectiveAmount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            effectiveAmount = BigDecimal.valueOf(0.01);
        }

        // Resolve gateway de pagamento (padrão: Xingubit Pay)
        PaymentGatewayResolver.ResolvedGateway resolved = gatewayResolver.resolve(PaymentGatewayType.XINGUBIT_PAY);
        PaymentGateway gateway = resolved.gateway();

        CreateChargeRequest chargeRequest = CreateChargeRequest.builder()
                .invoiceId(invoiceId)
                .contractId(contract.getId())
                .customerId(customer.getId())
                .customerName(customer.getName())
                .customerCpf(customer.getCpf())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhone())
                .amount(effectiveAmount)
                .dueDate(dueDate)
                .description("Mensalidade de Internet - Contrato " + contract.getContractNumber())
                .build();

        CreateChargeResponse chargeResponse = gateway.createCharge(chargeRequest, resolved.config());

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .contractId(contract.getId())
                .customerId(customer.getId())
                .gatewayConfigId(resolved.config().getId())
                .gatewayType(chargeResponse.getGatewayType())
                .externalTransactionId(chargeResponse.getExternalTransactionId())
                .amount(effectiveAmount)
                .dueDate(dueDate)
                .status(Invoice.InvoiceStatus.PENDING)
                .pixCopiaECola(chargeResponse.getPixCopiaECola())
                .pixQrCodeUrl(chargeResponse.getPixQrCodeUrl())
                .barcode(chargeResponse.getBarcode())
                .digitableLine(chargeResponse.getDigitableLine())
                .pdfUrl(chargeResponse.getPdfUrl())
                .compositionItems(new ArrayList<>())
                .build();

        Plan plan = (planRepository != null && contract.getPlanId() != null)
                ? planRepository.findById(contract.getPlanId()).orElse(null)
                : null;

        if (plan != null && plan.getCompositionItems() != null && !plan.getCompositionItems().isEmpty()) {
            for (PlanCompositionItem pItem : plan.getCompositionItems()) {
                invoice.getCompositionItems().add(InvoiceCompositionItem.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .invoice(invoice)
                        .name(pItem.getName())
                        .category(pItem.getCategory() != null ? pItem.getCategory() : PlanCompositionCategory.SCM)
                        .amount(pItem.getAmount())
                        .fiscalCode(pItem.getFiscalCode())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        } else {
            invoice.getCompositionItems().add(InvoiceCompositionItem.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .invoice(invoice)
                    .name("Conectividade Fibra Óptica SCM - Contrato " + contract.getContractNumber())
                    .category(PlanCompositionCategory.SCM)
                    .amount(contract.getMonthlyFee())
                    .fiscalCode("5307")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        if (proRataDelta.compareTo(BigDecimal.ZERO) != 0) {
            invoice.getCompositionItems().add(InvoiceCompositionItem.builder()
                    .id(UuidCreatorUtils.generateUuidV7())
                    .invoice(invoice)
                    .name("Ajuste Pro-Rata Die de Mudança de Plano")
                    .category(PlanCompositionCategory.SCM)
                    .amount(proRataDelta)
                    .fiscalCode("5307")
                    .createdAt(LocalDateTime.now())
                    .build());
            contract.setPendingProRataAdjustment(BigDecimal.ZERO);
            if (contractRepository != null) {
                contractRepository.save(contract);
            }
        }

        invoice.recalculateCompositionTotals();

        Invoice saved = invoiceRepository.save(invoice);

        // Registra log da transação
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .invoiceId(saved.getId())
                .gatewayType(saved.getGatewayType())
                .transactionType("CHARGE_CREATION")
                .status("CREATED")
                .build();
        transactionRepository.save(tx);

        // Emite evento INVOICE_GENERATED
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoiceId", saved.getId().toString());
        payload.put("contractId", contract.getId().toString());
        payload.put("customerId", customer.getId().toString());
        payload.put("customerName", customer.getName());
        payload.put("customerEmail", customer.getEmail());
        payload.put("customerPhone", customer.getPhone());
        payload.put("amount", saved.getAmount().toString());
        payload.put("dueDate", saved.getDueDate().toString());
        payload.put("pixCopiaECola", saved.getPixCopiaECola());
        payload.put("pixQrCodeUrl", saved.getPixQrCodeUrl());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("INVOICE_GENERATED")
                .aggregateType("Invoice")
                .aggregateId(saved.getId().toString())
                .payload(payload)
                .build();

        domainEventPublisher.publish(event);

        auditLogService.logAction("INVOICE_CREATED", "INVOICE", saved.getId().toString(),
                Map.of("contractId", contract.getId().toString(), "amount", saved.getAmount().toString(), "dueDate", saved.getDueDate().toString()));

        log.info("Fatura {} emitida com sucesso via {}. TxId={}", saved.getId(), saved.getGatewayType(), saved.getExternalTransactionId());
        return saved;
    }

    @Transactional
    public Invoice markInvoiceAsPaid(UUID invoiceId, @Nullable BigDecimal paidAmount, @Nullable String paymentMethod) {
        log.info("Marcando fatura {} como PAGA. Valor={}", invoiceId, paidAmount);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));

        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setPaidAmount(paidAmount != null ? paidAmount : invoice.getAmount());
        invoice.setPaymentMethod(paymentMethod != null ? paymentMethod : "PIX");

        Invoice saved = invoiceRepository.save(invoice);

        // Emite evento INVOICE_PAID
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoiceId", saved.getId().toString());
        payload.put("contractId", saved.getContractId().toString());
        payload.put("customerId", saved.getCustomerId().toString());
        payload.put("amount", saved.getAmount().toString());
        payload.put("paidAmount", saved.getPaidAmount().toString());
        payload.put("paidAt", saved.getPaidAt().toString());
        payload.put("paymentMethod", saved.getPaymentMethod());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("INVOICE_PAID")
                .aggregateType("Invoice")
                .aggregateId(saved.getId().toString())
                .payload(payload)
                .build();

        domainEventPublisher.publish(event);

        auditLogService.logAction("INVOICE_PAID", "INVOICE", saved.getId().toString(),
                Map.of("paidAmount", saved.getPaidAmount().toString(), "paymentMethod", saved.getPaymentMethod()));

        if (billingNotificationDunningService != null) {
            billingNotificationDunningService.dispatchPaymentConfirmed(saved);
        }

        return saved;
    }

    @Transactional
    public Invoice cancelInvoice(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));

        invoice.setStatus(Invoice.InvoiceStatus.CANCELED);
        Invoice saved = invoiceRepository.save(invoice);

        auditLogService.logAction("INVOICE_CANCELLED", "INVOICE", invoiceId.toString(),
                Map.of("action", "CANCEL_INVOICE"));

        return saved;
    }
}
