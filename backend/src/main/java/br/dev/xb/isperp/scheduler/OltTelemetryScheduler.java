package br.dev.xb.isperp.scheduler;

import br.dev.xb.isperp.service.FtthCorrelationEngine;
import br.dev.xb.isperp.service.OltTelemetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OltTelemetryScheduler {

    private final OltTelemetryService telemetryService;
    private final FtthCorrelationEngine correlationEngine;

    /**
     * Ciclo de Summary Polling leve a cada 2 minutos (120000 ms).
     */
    @Scheduled(fixedDelay = 120000, initialDelay = 30000)
    public void runScheduledTelemetryCycle() {
        try {
            log.debug("Iniciando ciclo de Summary Polling das portas PON...");
            telemetryService.pollOltPonSummaries();
            correlationEngine.runCorrelationAnalysis();
        } catch (Exception e) {
            log.error("Erro no ciclo agendado de telemetria OLT: {}", e.getMessage(), e);
        }
    }
}
