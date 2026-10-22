package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.BillingNotificationDispatch;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.repository.BillingNotificationDispatchRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null", "java:S2583"})
public class BillingNotificationDunningService {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BillingNotificationDispatchRepository dispatchRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final NotificationConfigRepository notificationConfigRepository;

    public record SweepResult(
            int totalDispatched,
            int dMinus3Count,
            int dZeroCount,
            int dPlus5AnatelCount,
            OffsetDateTime processedAt,
            List<BillingNotificationDispatch> dispatches
    ) {}

    /**
     * Executa a varredura ativa da régua omnichannel (D-3, D0 e D+5 Aviso Prévio RGC Anatel)
     * com idempotência estrita por (invoice_id, stage, channel).
     */
    @Transactional
    public SweepResult runOmnichannelDunningNotifications(LocalDate referenceDate) {
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now();
        List<Invoice> candidates = invoiceRepository.findAll().stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING
                        || i.getStatus() == Invoice.InvoiceStatus.OVERDUE)
                .toList();

        Set<String> activeChannels = resolveActiveChannels();
        List<BillingNotificationDispatch> dispatched = new ArrayList<>();
        int dMinus3Count = 0;
        int dZeroCount = 0;
        int dPlus5AnatelCount = 0;

        for (Invoice invoice : candidates) {
            if (invoice.getDueDate() == null) continue;

            BillingNotificationDispatch.Stage stage = null;
            if (invoice.getDueDate().isEqual(today.plusDays(3))) {
                stage = BillingNotificationDispatch.Stage.D_MINUS_3;
            } else if (invoice.getDueDate().isEqual(today)) {
                stage = BillingNotificationDispatch.Stage.D_ZERO;
            } else if (!invoice.getDueDate().isAfter(today.minusDays(5))) {
                stage = BillingNotificationDispatch.Stage.D_PLUS_5_ANATEL_NOTICE;
            }

            if (stage == null) continue;

            Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);

            for (String channel : activeChannels) {
                if (dispatchRepository.existsByInvoiceIdAndStageAndChannel(invoice.getId(), stage, channel)) {
                    continue;
                }
                BillingNotificationDispatch record = createAndSaveDispatch(invoice, customer, stage, channel);
                dispatched.add(record);
                switch (stage) {
                    case D_MINUS_3 -> dMinus3Count++;
                    case D_ZERO -> dZeroCount++;
                    case D_PLUS_5_ANATEL_NOTICE -> dPlus5AnatelCount++;
                    default -> {}
                }
            }
        }

        return new SweepResult(
                dispatched.size(),
                dMinus3Count,
                dZeroCount,
                dPlus5AnatelCount,
                OffsetDateTime.now(),
                dispatched
        );
    }

    /**
     * Dispara confirmação imediata de pagamento e liberação de velocidade (idempotente).
     */
    @Transactional
    public void dispatchPaymentConfirmed(Invoice invoice) {
        if (invoice == null || invoice.getId() == null) return;
        Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);
        Set<String> channels = resolveActiveChannels();
        for (String channel : channels) {
            if (!dispatchRepository.existsByInvoiceIdAndStageAndChannel(
                    invoice.getId(),
                    BillingNotificationDispatch.Stage.PAYMENT_CONFIRMED,
                    channel
            )) {
                createAndSaveDispatch(
                        invoice,
                        customer,
                        BillingNotificationDispatch.Stage.PAYMENT_CONFIRMED,
                        channel
                );
            }
        }
    }

    /**
     * Dispara ou reenvia notificação ativa de cobrança para uma fatura específica.
     */
    @Transactional
    public BillingNotificationDispatch sendManualInvoiceNotification(UUID invoiceId, String requestedChannel) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada: " + invoiceId));
        Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);

        String channel = (requestedChannel != null && !requestedChannel.isBlank())
                ? requestedChannel.trim().toUpperCase()
                : "WHATSAPP";

        LocalDate today = LocalDate.now();
        BillingNotificationDispatch.Stage stage;
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            stage = BillingNotificationDispatch.Stage.PAYMENT_CONFIRMED;
        } else if (invoice.getDueDate() != null && !invoice.getDueDate().isAfter(today.minusDays(5))) {
            stage = BillingNotificationDispatch.Stage.D_PLUS_5_ANATEL_NOTICE;
        } else if (invoice.getDueDate() != null && invoice.getDueDate().isAfter(today)) {
            stage = BillingNotificationDispatch.Stage.D_MINUS_3;
        } else {
            stage = BillingNotificationDispatch.Stage.D_ZERO;
        }

        Optional<BillingNotificationDispatch> existing = dispatchRepository
                .findByInvoiceIdAndStageAndChannel(invoice.getId(), stage, channel);

        String customerName = customer != null ? customer.getName() : "Assinante";
        String recipient = resolveRecipient(customer, channel);
        String message = buildStageMessage(invoice, customerName, stage);

        if (existing.isPresent()) {
            BillingNotificationDispatch updated = existing.get();
            updated.setRecipient(recipient);
            updated.setMessagePreview(message);
            updated.setDeliveryStatus("SENT");
            updated.setDispatchedAt(OffsetDateTime.now());
            return dispatchRepository.save(updated);
        }

        return createAndSaveDispatch(invoice, customer, stage, channel);
    }

    @Transactional(readOnly = true)
    public List<BillingNotificationDispatch> listDispatches() {
        return dispatchRepository.findAllByOrderByDispatchedAtDesc();
    }

    private Set<String> resolveActiveChannels() {
        Set<String> channels = new LinkedHashSet<>();
        List<NotificationConfig> activeConfigs = notificationConfigRepository.findByActiveTrue();
        for (NotificationConfig cfg : activeConfigs) {
            String nameUpper = cfg.getName() != null ? cfg.getName().toUpperCase() : "";
            if (nameUpper.contains("EMAIL") || nameUpper.contains("E-MAIL")) {
                channels.add("EMAIL");
            } else if (nameUpper.contains("SMS")) {
                channels.add("SMS");
            } else {
                channels.add("WHATSAPP");
            }
        }
        if (channels.isEmpty()) {
            channels.add("WHATSAPP");
        }
        return channels;
    }

    private BillingNotificationDispatch createAndSaveDispatch(
            Invoice invoice,
            Customer customer,
            BillingNotificationDispatch.Stage stage,
            String channel
    ) {
        String customerName = customer != null && customer.getName() != null
                ? customer.getName()
                : "Assinante";
        String recipient = resolveRecipient(customer, channel);
        String message = buildStageMessage(invoice, customerName, stage);

        BillingNotificationDispatch dispatch = BillingNotificationDispatch.builder()
                .invoiceId(invoice.getId())
                .contractId(invoice.getContractId())
                .customerId(invoice.getCustomerId())
                .customerName(customerName)
                .stage(stage)
                .channel(channel)
                .recipient(recipient)
                .messagePreview(message)
                .deliveryStatus("SENT")
                .dispatchedAt(OffsetDateTime.now())
                .build();

        return dispatchRepository.save(dispatch);
    }

    private String resolveRecipient(Customer customer, String channel) {
        if (customer == null) return "N/A";
        if ("EMAIL".equalsIgnoreCase(channel) && customer.getEmail() != null && !customer.getEmail().isBlank()) {
            return customer.getEmail();
        }
        if (customer.getPhone() != null && !customer.getPhone().isBlank()) {
            return customer.getPhone();
        }
        if (customer.getEmail() != null && !customer.getEmail().isBlank()) {
            return customer.getEmail();
        }
        return customer.getCpf() != null ? customer.getCpf() : "N/A";
    }

    private String buildStageMessage(
            Invoice invoice,
            String customerName,
            BillingNotificationDispatch.Stage stage
    ) {
        String amountStr = String.format("R$ %.2f", invoice.getAmount());
        String dueStr = invoice.getDueDate() != null ? invoice.getDueDate().format(BR_DATE) : "";
        String pixSnippet = invoice.getPixCopiaECola() != null && !invoice.getPixCopiaECola().isBlank()
                ? invoice.getPixCopiaECola()
                : "00020126580014br.gov.bcb.pix0136" + invoice.getId();

        return switch (stage) {
            case D_MINUS_3 -> String.format(
                    "Olá %s! Lembrete amigável: sua fatura de internet (%s) vence em %s. Pague com 1 clique via PIX Copia-e-Cola: %s",
                    customerName, amountStr, dueStr, pixSnippet
            );
            case D_ZERO -> String.format(
                    "Olá %s! Sua fatura (%s) vence HOJE (%s). Evite encargos pagando via PIX Copia-e-Cola: %s",
                    customerName, amountStr, dueStr, pixSnippet
            );
            case D_PLUS_5_ANATEL_NOTICE -> String.format(
                    "AVISO REGULATÓRIO ANATEL (Res. 632/765): Olá %s, identificamos pendência na fatura vencida em %s (%s). Regularize via PIX para evitar redução/suspensão parcial dos serviços: %s",
                    customerName, dueStr, amountStr, pixSnippet
            );
            case PAYMENT_CONFIRMED -> String.format(
                    "Pagamento Confirmado! Olá %s, recebemos a quitação da sua fatura (%s). Sua conexão fibra óptica está 100%% liberada!",
                    customerName, amountStr
            );
        };
    }
}
