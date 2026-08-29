package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceRebalanceService {

    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Verifica se o pagamento recebido foi feito fora de ordem (pagou fatura futura com fatura anterior aberta).
     */
    @Transactional
    public boolean checkAndHandleOutOfOrderPayment(@NonNull Invoice paidInvoice) {
        if (paidInvoice.getStatus() != Invoice.InvoiceStatus.PAID) {
            return false;
        }

        List<Invoice> contractInvoices = invoiceRepository.findByContractIdOrderByDueDateDesc(paidInvoice.getContractId());

        // Procura se existe alguma fatura em aberto com data de vencimento ANTERIOR à fatura que acabou de ser paga
        Invoice olderOpenInvoice = contractInvoices.stream()
                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PENDING || inv.getStatus() == Invoice.InvoiceStatus.OVERDUE)
                .filter(inv -> inv.getDueDate().isBefore(paidInvoice.getDueDate()))
                .findFirst()
                .orElse(null);

        if (olderOpenInvoice != null) {
            log.warn("Detectado pagamento invertido/fora de ordem! Cliente pagou fatura futura de {} enquanto a fatura de {} está em aberto.",
                    paidInvoice.getDueDate(), olderOpenInvoice.getDueDate());

            // 1. Protege imediatamente a fatura anterior contra qualquer corte ou bloqueio
            olderOpenInvoice.setProtectedAgainstSuspension(true);
            invoiceRepository.save(olderOpenInvoice);

            // 2. Dispara evento para notificar Dona Maria no WhatsApp com opções interativas
            Map<String, Object> payload = new HashMap<>();
            payload.put("contractId", paidInvoice.getContractId().toString());
            payload.put("customerId", paidInvoice.getCustomerId().toString());
            payload.put("futurePaidInvoiceId", paidInvoice.getId().toString());
            payload.put("futurePaidDueDate", paidInvoice.getDueDate().toString());
            payload.put("overdueUnpaidInvoiceId", olderOpenInvoice.getId().toString());
            payload.put("overdueUnpaidDueDate", olderOpenInvoice.getDueDate().toString());
            payload.put("amount", paidInvoice.getAmount());

            GenericDomainEvent event = GenericDomainEvent.builder()
                    .eventId(UuidCreatorUtils.generateUuidV7())
                    .eventType("OUT_OF_ORDER_PAYMENT_DETECTED")
                    .aggregateType("Invoice")
                    .aggregateId(paidInvoice.getId().toString())
                    .payload(payload)
                    .build();

            eventPublisher.publish(event);
            return true;
        }

        return false;
    }

    /**
     * Executa a compensação cruzada / rebalanceamento contábil autônomo:
     * - Baixa a fatura atrasada com o valor recebido.
     * - Reabre a fatura futura com seu vencimento original e isenção de encargos.
     * - Grava os avisos fixos explicativos em ambas as faturas para a Central do Assinante.
     */
    @Transactional
    public void executeCrossCreditRebalance(@NonNull UUID futurePaidInvoiceId, @NonNull UUID overdueUnpaidInvoiceId) {
        Invoice futureInvoice = invoiceRepository.findById(futurePaidInvoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura futura não encontrada"));
        Invoice overdueInvoice = invoiceRepository.findById(overdueUnpaidInvoiceId)
                .orElseThrow(() -> new RuntimeException("Fatura pendente não encontrada"));

        log.info("Executando compensação cruzada: transferindo pagamento da fatura {} ({}) para quitar a fatura {} ({})",
                futureInvoice.getId(), futureInvoice.getDueDate(), overdueInvoice.getId(), overdueInvoice.getDueDate());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String paymentDateStr = futureInvoice.getPaidAt() != null ? futureInvoice.getPaidAt().format(dtf) : LocalDateTime.now().format(dtf);

        // 1. Quita a fatura anterior
        overdueInvoice.setStatus(Invoice.InvoiceStatus.PAID);
        overdueInvoice.setPaidAt(LocalDateTime.now());
        overdueInvoice.setPaidAmount(futureInvoice.getPaidAmount() != null ? futureInvoice.getPaidAmount() : futureInvoice.getAmount());
        overdueInvoice.setPaymentMethod("COMPENSACAO_CRUZADA");
        overdueInvoice.setPaidByCrossCreditId(futureInvoice.getId());
        overdueInvoice.setProtectedAgainstSuspension(false);
        overdueInvoice.setRebalanceNotice(
                String.format("Esta fatura foi quitada automaticamente através do remanejamento do pagamento recebido na Fatura de %s (paga antecipadamente em %s). Nenhuma pendência restante.",
                        futureInvoice.getDueDate().format(dtf), paymentDateStr)
        );
        invoiceRepository.save(overdueInvoice);

        // 2. Reabre a fatura futura com vencimento original sem encargos
        futureInvoice.setStatus(Invoice.InvoiceStatus.PENDING);
        futureInvoice.setPaidAt(null);
        futureInvoice.setPaidAmount(null);
        futureInvoice.setPenaltyAmount(java.math.BigDecimal.ZERO);
        futureInvoice.setInterestAmount(java.math.BigDecimal.ZERO);
        futureInvoice.setRebalanceNotice(
                String.format("O pagamento original desta fatura foi utilizado para quitar a fatura pendente de %s. Esta fatura foi reaberta para pagamento regular em seu vencimento (%s) com isenção total de encargos.",
                        overdueInvoice.getDueDate().format(dtf), futureInvoice.getDueDate().format(dtf))
        );
        invoiceRepository.save(futureInvoice);

        // Garante que o contrato esteja ATIVO
        contractRepository.findById(overdueInvoice.getContractId()).ifPresent(c -> {
            if (c.getStatus() == Contract.ContractStatus.SUSPENDED) {
                c.setStatus(Contract.ContractStatus.ACTIVE);
                contractRepository.save(c);
            }
        });

        log.info("Compensação cruzada concluída com sucesso entre faturas {} e {}", overdueInvoice.getId(), futureInvoice.getId());
    }
}
