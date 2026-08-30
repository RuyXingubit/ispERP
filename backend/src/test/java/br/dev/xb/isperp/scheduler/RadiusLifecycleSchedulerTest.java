package br.dev.xb.isperp.scheduler;

import br.dev.xb.isperp.dto.RadiusPolicyConfigResponse;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.TrustUnblock;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.TrustUnblockRepository;
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

    private RadiusLifecycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RadiusLifecycleScheduler(
                radiusLifecycleService,
                contractRepository,
                invoiceRepository,
                trustUnblockRepository
        );
    }

    @Test
    @DisplayName("Deve bloquear contrato inadimplente com fatura vencida além da tolerância")
    void testProcessAutoBlockRoutineExecutesBlock() {
        UUID customerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        RadiusPolicyConfigResponse config = RadiusPolicyConfigResponse.builder()
                .autoBlockEnabled(true)
                .toleranceDays(5)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-001")
                .customerId(customerId)
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        Invoice overdueInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .dueDate(LocalDate.now().minusDays(10)) // 10 dias de atraso (> 5 dias de tolerância)
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        when(radiusLifecycleService.getPolicyConfigResponse()).thenReturn(config);
        when(contractRepository.findByStatusOrderByCreatedAtDesc(Contract.ContractStatus.ACTIVE)).thenReturn(List.of(contract));
        when(invoiceRepository.findByCustomerIdAndStatus(customerId, Invoice.InvoiceStatus.PENDING)).thenReturn(List.of(overdueInvoice));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId)).thenReturn(List.of());

        scheduler.processAutoBlockRoutine();

        verify(radiusLifecycleService).executeAutoBlock(eq(contractId), contains("Auto-corte por inadimplência"));
    }

    @Test
    @DisplayName("Deve ignorar contrato se houver Desbloqueio em Confiança ativo")
    void testProcessAutoBlockSkipsTrustUnblock() {
        UUID customerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        RadiusPolicyConfigResponse config = RadiusPolicyConfigResponse.builder()
                .autoBlockEnabled(true)
                .toleranceDays(5)
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-002")
                .customerId(customerId)
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        Invoice overdueInvoice = Invoice.builder()
                .id(UUID.randomUUID())
                .customerId(customerId)
                .dueDate(LocalDate.now().minusDays(10))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        TrustUnblock activeUnblock = TrustUnblock.builder()
                .id(UUID.randomUUID())
                .contractId(contractId)
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusHours(12))
                .build();

        when(radiusLifecycleService.getPolicyConfigResponse()).thenReturn(config);
        when(contractRepository.findByStatusOrderByCreatedAtDesc(Contract.ContractStatus.ACTIVE)).thenReturn(List.of(contract));
        when(invoiceRepository.findByCustomerIdAndStatus(customerId, Invoice.InvoiceStatus.PENDING)).thenReturn(List.of(overdueInvoice));
        when(trustUnblockRepository.findByContractIdOrderByRequestedAtDesc(contractId)).thenReturn(List.of(activeUnblock));

        scheduler.processAutoBlockRoutine();

        verify(radiusLifecycleService, never()).executeAutoBlock(any(), any());
    }
}
