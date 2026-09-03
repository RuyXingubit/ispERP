package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.SentinelAuditLogDto;
import br.dev.xb.isperp.entity.financial.FeeStatus;
import br.dev.xb.isperp.entity.financial.SentinelAuditLog;
import br.dev.xb.isperp.entity.financial.SentinelSeverity;
import br.dev.xb.isperp.entity.financial.UserCashCustody;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.repository.financial.SentinelAuditLogRepository;
import br.dev.xb.isperp.repository.financial.UserCashCustodyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SentinelWatchdogService {

    private final SentinelAuditLogRepository auditLogRepository;
    private final UserCashCustodyRepository cashCustodyRepository;
    private final WorkOrderRepository workOrderRepository;

    private static final BigDecimal CASH_CONCENTRATION_THRESHOLD = new BigDecimal("1000.00");

    @Transactional
    public List<SentinelAuditLogDto> triggerManualSweep() {
        log.info("Iniciando varredura pericial do Sentinela IA...");
        List<SentinelAuditLog> newAlerts = new ArrayList<>();

        // 1. Auditoria de Concentração de Caixa Vivo por CPF
        List<UserCashCustody> custodies = cashCustodyRepository.findAll();
        for (UserCashCustody c : custodies) {
            if (c.getCurrentBalance() != null && c.getCurrentBalance().compareTo(CASH_CONCENTRATION_THRESHOLD) > 0) {
                SentinelAuditLog alert = SentinelAuditLog.builder()
                        .auditType("CASH_CONCENTRATION")
                        .severity(SentinelSeverity.HIGH)
                        .title("Concentração Atípica de Dinheiro Vivo: " + c.getUser().getName())
                        .description(String.format("O colaborador %s (CPF: %s) possui R$ %s retidos em mãos.",
                                c.getUser().getName(), c.getUser().getCpf(), c.getCurrentBalance()))
                        .geminiAnalysis("Padrão de risco: Retenção prolongada de valores sem prestação de contas na tesouraria ou depósito bancário. Risco de perda patrimonial ou apropriação indevida.")
                        .recommendedAction("Exigir prestação de contas imediata ou depósito com comprovante na esteira de conciliação do CFO.")
                        .resolved(false)
                        .build();
                newAlerts.add(alert);
            }
        }

        // 2. Auditoria de Volume de Solicitações de Isenção de Taxas de O.S.
        long pendingWaiversCount = workOrderRepository.findAll().stream()
                .filter(wo -> wo.getFeeStatus() == FeeStatus.PENDING_WAIVER_APPROVAL)
                .count();

        if (pendingWaiversCount > 3) {
            SentinelAuditLog alert = SentinelAuditLog.builder()
                    .auditType("FEE_WAIVER_ANOMALY")
                    .severity(SentinelSeverity.MEDIUM)
                    .title("Pico de Solicitações de Isenção de Taxas (" + pendingWaiversCount + " O.S.)")
                    .description(String.format("Existem %d Ordens de Serviço aguardando decisão gerencial de isenção de taxas.", pendingWaiversCount))
                    .geminiAnalysis("Padrão comercial: Atendentes podem estar utilizando a isenção de forma banalizada ou colaboradores de campo podem estar tentando negociar pagamentos em dinheiro por fora.")
                    .recommendedAction("Auditar a esteira de isenções no painel do CFO. Para as isenções aprovadas, o sistema disparará o comunicado anti-fraude via WhatsApp ao cliente.")
                    .resolved(false)
                    .build();
            newAlerts.add(alert);
        }

        auditLogRepository.saveAll(newAlerts);
        log.info("Varredura do Sentinela IA concluída. Alertas identificados: {}", newAlerts.size());

        return getActiveAuditAlerts();
    }

    @Transactional(readOnly = true)
    public List<SentinelAuditLogDto> getActiveAuditAlerts() {
        return auditLogRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SentinelAuditLogDto resolveAuditAlert(UUID alertId) {
        SentinelAuditLog logItem = auditLogRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta de auditoria não encontrado: " + alertId));

        logItem.setResolved(true);
        SentinelAuditLog saved = auditLogRepository.save(logItem);
        log.info("Alerta do Sentinela IA resolvido: {}", saved.getTitle());
        return toDto(saved);
    }

    private SentinelAuditLogDto toDto(SentinelAuditLog entity) {
        return SentinelAuditLogDto.builder()
                .id(entity.getId())
                .auditType(entity.getAuditType())
                .severity(entity.getSeverity())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .geminiAnalysis(entity.getGeminiAnalysis())
                .recommendedAction(entity.getRecommendedAction())
                .resolved(entity.getResolved())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
