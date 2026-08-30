package br.dev.xb.isperp.scheduler;

import br.dev.xb.isperp.dto.RadiusPolicyConfigResponse;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.TrustUnblockRepository;
import br.dev.xb.isperp.service.RadiusLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RadiusLifecycleScheduler {

    private final RadiusLifecycleService radiusLifecycleService;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final TrustUnblockRepository trustUnblockRepository;

    /**
     * Executa a rotina periódica de corte por inadimplência às 06:00 e 12:00
     */
    @Scheduled(cron = "${radius.autoblock.cron:0 0 6,12 * * *}")
    public void processAutoBlockRoutine() {
        RadiusPolicyConfigResponse config = radiusLifecycleService.getPolicyConfigResponse();
        if (!config.isAutoBlockEnabled()) {
            log.info("Rotina de auto-corte RADIUS desabilitada nas configurações.");
            return;
        }

        log.info("Iniciando rotina de auto-corte RADIUS (tolerância: {} dias)...", config.getToleranceDays());

        LocalDate cutoffDate = LocalDate.now().minusDays(config.getToleranceDays());
        List<Contract> activeContracts = contractRepository.findByStatusOrderByCreatedAtDesc(Contract.ContractStatus.ACTIVE);

        int blockedCount = 0;
        int skippedCount = 0;

        for (Contract contract : activeContracts) {
            try {
                // Busca faturas vencidas além da data limite de tolerância
                List<Invoice> overdueInvoices = invoiceRepository.findByCustomerIdAndStatus(contract.getCustomerId(), Invoice.InvoiceStatus.PENDING)
                        .stream()
                        .filter(inv -> inv.getDueDate().isBefore(cutoffDate))
                        .sorted((a, b) -> a.getDueDate().compareTo(b.getDueDate()))
                        .toList();

                if (overdueInvoices.isEmpty()) {
                    continue;
                }

                // Verifica se há Desbloqueio em Confiança ainda ativo
                List<TrustUnblock> unblocks = trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contract.getId());
                boolean hasActiveTrustUnblock = unblocks.stream()
                        .anyMatch(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()) && u.getExpiresAt().isAfter(LocalDateTime.now()));

                if (hasActiveTrustUnblock) {
                    log.info("Contrato {} ignorado pelo auto-corte: Desbloqueio em Confiança ativo.", contract.getContractNumber());
                    skippedCount++;
                    continue;
                }

                Invoice oldestInvoice = overdueInvoices.get(0);
                String reason = "Auto-corte por inadimplência (Fatura vencida em " + oldestInvoice.getDueDate() + ")";

                radiusLifecycleService.executeAutoBlock(contract.getId(), reason);
                blockedCount++;

            } catch (Exception e) {
                log.error("Erro ao processar auto-corte para contrato {}: {}", contract.getId(), e.getMessage());
            }
        }

        log.info("Rotina de auto-corte RADIUS concluída: {} contratos bloqueados, {} contratos em carência/confiança.",
                blockedCount, skippedCount);
    }
}
