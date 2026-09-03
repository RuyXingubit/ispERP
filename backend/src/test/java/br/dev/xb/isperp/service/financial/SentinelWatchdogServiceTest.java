package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.financial.SentinelAuditLog;
import br.dev.xb.isperp.entity.financial.SentinelSeverity;
import br.dev.xb.isperp.entity.financial.UserCashCustody;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.repository.financial.SentinelAuditLogRepository;
import br.dev.xb.isperp.repository.financial.UserCashCustodyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentinelWatchdogServiceTest {

    @Mock
    private SentinelAuditLogRepository auditLogRepository;

    @Mock
    private UserCashCustodyRepository cashCustodyRepository;

    @Mock
    private WorkOrderRepository workOrderRepository;

    private SentinelWatchdogService watchdogService;

    @BeforeEach
    void setUp() {
        watchdogService = new SentinelWatchdogService(
                auditLogRepository,
                cashCustodyRepository,
                workOrderRepository
        );
    }

    @Test
    @DisplayName("Sentinela deve disparar alerta de alta gravidade se colaborador retiver mais de R$ 1.000 em dinheiro vivo")
    void shouldFlagCashConcentrationAnomaly() {
        User technician = User.builder()
                .id(UUID.randomUUID())
                .name("Carlos Técnico")
                .cpf("12345678901")
                .build();

        UserCashCustody custody = UserCashCustody.builder()
                .id(UUID.randomUUID())
                .user(technician)
                .currentBalance(new BigDecimal("2500.00")) // R$ 2.500 retidos em mãos
                .build();

        when(cashCustodyRepository.findAll()).thenReturn(List.of(custody));
        when(workOrderRepository.findAll()).thenReturn(List.of());

        watchdogService.triggerManualSweep();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SentinelAuditLog>> captor = ArgumentCaptor.forClass(List.class);
        verify(auditLogRepository).saveAll(captor.capture());

        List<SentinelAuditLog> saved = captor.getValue();
        assertThat(saved).hasSize(1);
        SentinelAuditLog alert = saved.get(0);
        assertThat(alert.getAuditType()).isEqualTo("CASH_CONCENTRATION");
        assertThat(alert.getSeverity()).isEqualTo(SentinelSeverity.HIGH);
        assertThat(alert.getDescription()).contains("R$ 2500.00");
    }
}
