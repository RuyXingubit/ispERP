package br.dev.xb.isperp.scheduler;

import br.dev.xb.isperp.service.FiscalRegimeTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FiscalRegimeScheduler {

    private final FiscalRegimeTransitionService regimeTransitionService;

    /**
     * Executa diariamente à meia-noite (00:01) para aplicar transições fiscais cuja data de vigência chegou.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void processDailyRegimeTransitions() {
        log.info("⏰ [FiscalRegimeScheduler] Checando transições de regime fiscal programadas para hoje...");
        int applied = regimeTransitionService.applyPendingTransitions();
        if (applied > 0) {
            log.info("✅ [FiscalRegimeScheduler] {} transições de regime fiscal foram aplicadas com sucesso.", applied);
        } else {
            log.debug("ℹ️ [FiscalRegimeScheduler] Nenhuma transição fiscal pendente para aplicação hoje.");
        }
    }

    /**
     * Executa também na inicialização do sistema para garantir que transições passadas durante downtime sejam aplicadas.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupCheckTransitions() {
        log.info("🚀 [FiscalRegimeScheduler] Verificação inicial de transições de regime fiscal agendadas no startup...");
        int applied = regimeTransitionService.applyPendingTransitions();
        if (applied > 0) {
            log.info("✅ [FiscalRegimeScheduler] {} transições agendadas foram sincronizadas na inicialização.", applied);
        }
    }
}
