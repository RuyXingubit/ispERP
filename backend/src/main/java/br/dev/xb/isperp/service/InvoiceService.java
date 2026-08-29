package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.PaymentTransaction;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.gateway.PaymentGateway;
import br.dev.xb.isperp.gateway.PaymentGatewayResolver;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.gateway.dto.CreateChargeRequest;
import br.dev.xb.isperp.gateway.dto.CreateChargeResponse;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.PaymentTransactionRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentGatewayResolver gatewayResolver;
    private final DomainEventPublisher domainEventPublisher;

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Invoice> getInvoicesByCustomerId(@NonNull UUID customerId) {
        return invoiceRepository.findByCustomerIdOrderByDueDateDesc(customerId);
    }

    public List<Invoice> getInvoicesByContractId(@NonNull UUID contractId) {
        return invoiceRepository.findByContractIdOrderByDueDateDesc(contractId);
    }

    public List<Invoice> getInvoicesByStatus(@NonNull Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatusOrderByDueDateAsc(status);
    }

    public Optional<Invoice> getInvoiceById(@NonNull UUID id) {
        return invoiceRepository.findById(id);
    }

    public Optional<Invoice> getInvoiceByExternalTransactionId(@NonNull String txId) {
        return invoiceRepository.findByExternalTransactionId(txId);
    }

    @Transactional
    public Invoice createInvoiceForContract(@NonNull Contract contract, @NonNull LocalDate dueDate) {
        log.info("Gerando fatura para contrato {}: vencimento={}, valor={}",
                contract.getContractNumber(), dueDate, contract.getMonthlyFee());

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente do contrato não encontrado"));

        UUID invoiceId = UuidCreatorUtils.generateUuidV7();

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
                .amount(contract.getMonthlyFee())
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
                .amount(contract.getMonthlyFee())
                .dueDate(dueDate)
                .status(Invoice.InvoiceStatus.PENDING)
                .pixCopiaECola(chargeResponse.getPixCopiaECola())
                .pixQrCodeUrl(chargeResponse.getPixQrCodeUrl())
                .barcode(chargeResponse.getBarcode())
                .digitableLine(chargeResponse.getDigitableLine())
                .pdfUrl(chargeResponse.getPdfUrl())
                .build();

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

        log.info("Fatura {} emitida com sucesso via {}. TxId={}", saved.getId(), saved.getGatewayType(), saved.getExternalTransactionId());
        return saved;
    }

    @Transactional
    public Invoice markInvoiceAsPaid(@NonNull UUID invoiceId, BigDecimal paidAmount, String paymentMethod) {
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

        return saved;
    }

    @Transactional
    public Invoice cancelInvoice(@NonNull UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura não encontrada"));

        invoice.setStatus(Invoice.InvoiceStatus.CANCELED);
        return invoiceRepository.save(invoice);
    }
}
