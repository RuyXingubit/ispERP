package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class BillingScheduler {

    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    /**
     * Executa diariamente às 04:00 da manhã para gerar as faturas do ciclo dos contratos ATIVOS.
     */
    @Scheduled(cron = "${billing.scheduler.cron:0 0 4 * * ?}")
    public void generateMonthlyInvoices() {
        log.info("Iniciando rotina diária de faturamento recorrente...");

        List<Contract> activeContracts = contractRepository.findByStatusOrderByCreatedAtDesc(Contract.ContractStatus.ACTIVE);
        LocalDate today = LocalDate.now();

        int generatedCount = 0;
        for (Contract contract : activeContracts) {
            try {
                int dueDay = contract.getDueDay() != null ? contract.getDueDay() : 10;
                LocalDate targetDueDate = calculateDueDate(today, dueDay);

                // Verifica se já existe fatura emitida para este ciclo
                boolean alreadyExists = invoiceRepository.existsByContractIdAndDueDate(contract.getId(), targetDueDate);
                if (!alreadyExists) {
                    invoiceService.createInvoiceForContract(contract, targetDueDate);
                    generatedCount++;
                }
            } catch (Exception e) {
                log.error("Erro ao gerar fatura automática para contrato {}: {}", contract.getId(), e.getMessage());
            }
        }

        log.info("Rotina de faturamento concluída: {} faturas geradas para {} contratos ativos.",
                generatedCount, activeContracts.size());
    }

    private LocalDate calculateDueDate(LocalDate today, int dueDay) {
        int maxDayOfMonth = today.lengthOfMonth();
        int safeDay = Math.min(dueDay, maxDayOfMonth);

        LocalDate candidate = today.withDayOfMonth(safeDay);
        if (candidate.isBefore(today)) {
            LocalDate nextMonth = today.plusMonths(1);
            int safeDayNextMonth = Math.min(dueDay, nextMonth.lengthOfMonth());
            return nextMonth.withDayOfMonth(safeDayNextMonth);
        }
        return candidate;
    }
}
