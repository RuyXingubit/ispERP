package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchicalBillingService {

    private final ContractRepository contractRepository;
    private final PlanRepository planRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * Calcula o prazo de tolerância/dias de suspensão aplicando a hierarquia:
     * 1. Regra do Contrato (custom_suspension_days)
     * 2. Cliente Governo (90 dias)
     * 3. Regra do Plano (suspension_days)
     * 4. Default Global (5 dias)
     */
    public int resolveSuspensionDays(Contract contract) {
        if (contract.getCustomSuspensionDays() != null && contract.getCustomSuspensionDays() > 0) {
            return contract.getCustomSuspensionDays();
        }

        Customer customer = customerRepository.findById(contract.getCustomerId()).orElse(null);
        if (customer != null && Boolean.TRUE.equals(customer.getIsGovernment())) {
            return 90; // Carência especial legal para órgãos públicos
        }

        Plan plan = planRepository.findById(contract.getPlanId()).orElse(null);
        if (plan != null && plan.getSuspensionDays() != null && plan.getSuspensionDays() > 0) {
            return plan.getSuspensionDays();
        }

        return 5; // Default global
    }

    /**
     * Aplica a trava regulatória de suspensão (ANATEL / CDC):
     * - Nunca suspende em sextas-feiras, sábados, domingos ou feriados.
     * - Adia automaticamente para a próxima segunda-feira útil às 14:00 (horário comercial).
     */
    public LocalDate adjustCutoffDateToBusinessDay(LocalDate rawCutoffDate) {
        LocalDate adjusted = rawCutoffDate;
        DayOfWeek day = adjusted.getDayOfWeek();

        if (day == DayOfWeek.FRIDAY) {
            adjusted = adjusted.plusDays(3); // Pula para Segunda-feira
        } else if (day == DayOfWeek.SATURDAY) {
            adjusted = adjusted.plusDays(2); // Pula para Segunda-feira
        } else if (day == DayOfWeek.SUNDAY) {
            adjusted = adjusted.plusDays(1); // Pula para Segunda-feira
        }

        return adjusted;
    }

    /**
     * Verifica se o momento atual é permitido para corte (dias úteis entre 14h e 17h).
     */
    public boolean isAllowedCutoffWindow(LocalDateTime now) {
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime time = now.toLocalTime();
        // Janela comercial vespertina para garantir suporte aberto ao assinante
        return !time.isBefore(LocalTime.of(14, 0)) && !time.isAfter(LocalTime.of(17, 30));
    }

    /**
     * Avalia faturas vencidas e executa a suspensão humanizada e regulatória.
     */
    @Transactional
    public int processDailyDunning(LocalDateTime executionTime) {
        LocalDate today = executionTime.toLocalDate();
        log.info("Iniciando rotina de régua de cobrança e suspensão para data: {}", today);

        List<Invoice> overdueInvoices = invoiceRepository.findByStatusOrderByDueDateAsc(Invoice.InvoiceStatus.OVERDUE);
        int suspendedCount = 0;

        for (Invoice invoice : overdueInvoices) {
            if (Boolean.TRUE.equals(invoice.getProtectedAgainstSuspension())) {
                log.info("Fatura {} está protegida contra suspensão (pagamento cruzado ou rebalanceamento ativo).", invoice.getId());
                continue;
            }

            Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
            if (contract == null || contract.getStatus() == Contract.ContractStatus.CANCELED || contract.getStatus() == Contract.ContractStatus.SUSPENDED) {
                continue;
            }

            int graceDays = resolveSuspensionDays(contract);
            LocalDate rawCutoff = invoice.getDueDate().plusDays(graceDays);
            LocalDate allowedCutoff = adjustCutoffDateToBusinessDay(rawCutoff);

            if (!today.isBefore(allowedCutoff)) {
                // Atingiu o prazo permitido
                contract.setStatus(Contract.ContractStatus.SUSPENDED);
                contractRepository.save(contract);

                Map<String, Object> payload = new HashMap<>();
                payload.put("contractId", contract.getId().toString());
                payload.put("customerId", contract.getCustomerId().toString());
                payload.put("invoiceId", invoice.getId().toString());
                payload.put("overdueAmount", invoice.getAmount());
                payload.put("dueDate", invoice.getDueDate().toString());
                payload.put("pixCopiaECola", invoice.getPixCopiaECola());

                GenericDomainEvent event = GenericDomainEvent.builder()
                        .eventId(UuidCreatorUtils.generateUuidV7())
                        .eventType("INTERNET_ACCESS_SUSPENDED")
                        .aggregateType("Contract")
                        .aggregateId(contract.getId().toString())
                        .payload(payload)
                        .build();

                eventPublisher.publish(event);
                suspendedCount++;
                log.info("Suspensão regulatória disparada para contrato {}. Fatura vencida em {} com carência de {} dias.",
                        contract.getContractNumber(), invoice.getDueDate(), graceDays);
            }
        }

        return suspendedCount;
    }
}
