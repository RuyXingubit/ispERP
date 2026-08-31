package br.dev.xb.isperp.scheduler;

import br.dev.xb.isperp.dto.RadiusPolicyConfigResponse;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.RadiusPolicyConfig;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.RadiusPolicyConfigRepository;
import br.dev.xb.isperp.repository.TrustUnblockRepository;
import br.dev.xb.isperp.service.BrazilianCalendarService;
import br.dev.xb.isperp.service.RadiusLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RadiusLifecycleScheduler {

    private final RadiusLifecycleService radiusLifecycleService;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final TrustUnblockRepository trustUnblockRepository;
    private final RadiusPolicyConfigRepository policyConfigRepository;
    private final BrazilianCalendarService brazilianCalendarService;

    /**
     * Executa a rotina periódica de corte por inadimplência às 09:30 da manhã em dias úteis
     */
    @Scheduled(cron = "${radius.autoblock.cron:0 30 9 * * ?}")
    public void processAutoBlockRoutine() {
        RadiusPolicyConfig policyConfig = policyConfigRepository.findFirstConfig()
                .orElseGet(() -> RadiusPolicyConfig.builder().build());

        if (!policyConfig.isAutoBlockEnabled()) {
            log.info("Rotina de auto-corte RADIUS desabilitada nas configurações.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Validação estrita de Horário Comercial, Dias Úteis e Feriados
        if (!brazilianCalendarService.isAllowedForAutoBlock(today, now, policyConfig)) {
            log.info("Auto-corte ignorado: {} {} está fora da janela permitida (Horário Comercial {}h-{}h, Dias Úteis Seg-Qui, sem feriados ou vésperas).",
                    today, now, policyConfig.getBlockStartHour(), policyConfig.getBlockEndHour());
            return;
        }

        log.info("Iniciando rotina de auto-corte RADIUS (tolerância: {} dias)...", policyConfig.getToleranceDays());

        LocalDate cutoffDate = today.minusDays(policyConfig.getToleranceDays());
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
