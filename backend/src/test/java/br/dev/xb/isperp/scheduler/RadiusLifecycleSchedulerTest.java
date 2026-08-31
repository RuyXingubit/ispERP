package br.dev.xb.isperp.scheduler;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RadiusLifecycleSchedulerTest {

    @Mock
    private RadiusLifecycleService radiusLifecycleService;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TrustUnblockRepository trustUnblockRepository;

    @Mock
    private RadiusPolicyConfigRepository policyConfigRepository;

    @Mock
    private BrazilianCalendarService brazilianCalendarService;

    private RadiusLifecycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RadiusLifecycleScheduler(
                radiusLifecycleService,
                contractRepository,
                invoiceRepository,
                trustUnblockRepository,
                policyConfigRepository,
                brazilianCalendarService
        );
    }

    @Test
    @DisplayName("Deve bloquear contrato inadimplente em dia útil e horário comercial")
    void testProcessAutoBlockRoutineExecutesBlock() {
        UUID customerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        RadiusPolicyConfig config = RadiusPolicyConfig.builder()
                .autoBlockEnabled(true)
                .toleranceDays(5)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .contractNumber("CTR-001")
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .contractId(contractId)
                .dueDate(LocalDate.now().minusDays(10))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        when(policyConfigRepository.findFirstConfig()).thenReturn(Optional.of(config));
        when(brazilianCalendarService.isAllowedForAutoBlock(any(), any(), any())).thenReturn(true);
        when(contractRepository.findByStatusOrderByCreatedAtDesc(Contract.ContractStatus.ACTIVE))
                .thenReturn(List.of(contract));
        when(invoiceRepository.findByCustomerIdAndStatus(customerId, Invoice.InvoiceStatus.PENDING))
                .thenReturn(List.of(invoice));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId))
                .thenReturn(List.of());

        scheduler.processAutoBlockRoutine();

        verify(radiusLifecycleService).executeAutoBlock(eq(contractId), contains("Fatura vencida em"));
    }

    @Test
    @DisplayName("Deve ignorar rotina se fora do horário comercial ou feriado")
    void testProcessAutoBlockIgnoredWhenNotAllowedByCalendar() {
        RadiusPolicyConfig config = RadiusPolicyConfig.builder()
                .autoBlockEnabled(true)
                .toleranceDays(5)
                .build();

        when(policyConfigRepository.findFirstConfig()).thenReturn(Optional.of(config));
        when(brazilianCalendarService.isAllowedForAutoBlock(any(), any(), any())).thenReturn(false);

        scheduler.processAutoBlockRoutine();

        verifyNoInteractions(contractRepository);
        verifyNoInteractions(radiusLifecycleService);
    }

    @Test
    @DisplayName("Deve ignorar contrato se possuir Desbloqueio em Confiança ativo")
    void testProcessAutoBlockSkipsContractWithActiveTrustUnblock() {
        UUID customerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        RadiusPolicyConfig config = RadiusPolicyConfig.builder()
                .autoBlockEnabled(true)
                .toleranceDays(5)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .contractNumber("CTR-002")
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .contractId(contractId)
                .dueDate(LocalDate.now().minusDays(12))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        TrustUnblock trustUnblock = TrustUnblock.builder()
                .id(UUID.randomUUID())
                .contractId(contractId)
                .status("ACTIVE")
                .requestedAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(policyConfigRepository.findFirstConfig()).thenReturn(Optional.of(config));
        when(brazilianCalendarService.isAllowedForAutoBlock(any(), any(), any())).thenReturn(true);
        when(contractRepository.findByStatusOrderByCreatedAtDesc(Contract.ContractStatus.ACTIVE))
                .thenReturn(List.of(contract));
        when(invoiceRepository.findByCustomerIdAndStatus(customerId, Invoice.InvoiceStatus.PENDING))
                .thenReturn(List.of(invoice));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId))
                .thenReturn(List.of(trustUnblock));

        scheduler.processAutoBlockRoutine();

        verify(radiusLifecycleService, never()).executeAutoBlock(any(), any());
    }
}
