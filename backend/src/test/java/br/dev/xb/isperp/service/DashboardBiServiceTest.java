package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.DashboardBiDTO;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class DashboardBiServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private OnuProvisioningRepository onuRepository;

    @Mock
    private NetworkDeviceRepository networkDeviceRepository;

    @InjectMocks
    private DashboardBiService dashboardBiService;

    private UUID customerId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        customerId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve calcular corretamente métricas de MRR, ARR, ARPU, Churn e Inadimplência")
    void shouldCalculateExecutiveBiMetricsCorrectly() {
        // Mock Contratos: 2 ativos (100.00 e 200.00) e 1 cancelado
        Contract c1 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .planId(planId)
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("100.00"))
                .build();

        Contract c2 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .planId(planId)
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("200.00"))
                .build();

        Contract c3 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .planId(planId)
                .status(Contract.ContractStatus.CANCELED)
                .monthlyFee(new BigDecimal("150.00"))
                .build();

        when(contractRepository.findAll()).thenReturn(List.of(c1, c2, c3));
        when(customerRepository.count()).thenReturn(2L);

        // Mock Faturas: 1 paga (100.00) e 1 vencida (200.00)
        Invoice invPaid = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(c1.getId())
                .customerId(customerId)
                .amount(new BigDecimal("100.00"))
                .status(Invoice.InvoiceStatus.PAID)
                .dueDate(LocalDate.now().minusDays(5))
                .build();

        Invoice invOverdue = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(c2.getId())
                .customerId(customerId)
                .amount(new BigDecimal("200.00"))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .dueDate(LocalDate.now().minusDays(10))
                .build();

        when(invoiceRepository.findAll()).thenReturn(List.of(invPaid, invOverdue));

        // Mock ONUs: 1 normal (-19.00 dBm) e 1 crítica (-27.50 dBm)
        OnuProvisioning onuNormal = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .onuMac("AA:BB:CC:11:22:33")
                .onuSerial("HWTC0001")
                .rxPowerDbm(new BigDecimal("-19.00"))
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .build();

        OnuProvisioning onuCritical = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .onuMac("AA:BB:CC:44:55:66")
                .onuSerial("HWTC0002")
                .rxPowerDbm(new BigDecimal("-27.50"))
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .build();

        when(onuRepository.findAll()).thenReturn(List.of(onuNormal, onuCritical));
        when(networkDeviceRepository.count()).thenReturn(1L);

        // Execução
        DashboardBiDTO metrics = dashboardBiService.getDashboardMetrics();

        // Asserts Financeiros
        assertNotNull(metrics);
        assertEquals(new BigDecimal("300.00"), metrics.getMrr()); // 100 + 200
        assertEquals(new BigDecimal("3600.00"), metrics.getArr()); // 300 * 12
        assertEquals(new BigDecimal("150.00"), metrics.getArpu()); // 300 / 2 contratos ativos

        // Asserts Churn e Inadimplência
        assertEquals(new BigDecimal("33.33"), metrics.getChurnRate()); // 1 cancelado de 3 contratos totais = 33.33%
        assertEquals(new BigDecimal("200.00"), metrics.getOverdueAmount());
        assertEquals(new BigDecimal("66.67"), metrics.getDefaultRate()); // 200 de 300 faturados = 66.67%

        // Asserts NOC
        assertEquals(2, metrics.getTotalOnus());
        assertEquals(1, metrics.getCriticalSignalOnus()); // onuCritical com -27.50 dBm
    }
}
