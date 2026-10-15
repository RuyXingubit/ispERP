package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.FtthIncidentResponse;
import br.dev.xb.isperp.entity.BillingNotificationDispatch;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.FtthIncident;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.NotificationConfig;
import br.dev.xb.isperp.mapper.FtthMonitoringMapper;
import br.dev.xb.isperp.monitoring.IncidentSeverity;
import br.dev.xb.isperp.monitoring.IncidentStatus;
import br.dev.xb.isperp.monitoring.IncidentType;
import br.dev.xb.isperp.notification.whatsapp.WhatsAppProviderType;
import br.dev.xb.isperp.repository.BillingNotificationDispatchRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.FtthCableRepository;
import br.dev.xb.isperp.repository.FtthCtoPortRepository;
import br.dev.xb.isperp.repository.FtthCtoRepository;
import br.dev.xb.isperp.repository.FtthIncidentRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.NetworkDeviceRepository;
import br.dev.xb.isperp.repository.NotificationConfigRepository;
import br.dev.xb.isperp.repository.OltPonPortRepository;
import br.dev.xb.isperp.repository.OnuProvisioningRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BillingNotificationDunningServiceTest {

    @Mock
    private BillingNotificationDispatchRepository dispatchRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private NotificationConfigRepository notificationConfigRepository;

    @InjectMocks
    private BillingNotificationDunningService dunningService;

    @Test
    void runOmnichannelDunningNotifications_DispatchesDMinus3DZeroAndDPlus5WithIdempotency() {
        LocalDate refDate = LocalDate.of(2026, 10, 14);
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        UUID contractId = UuidCreatorUtils.generateUuidV7();

        Customer customer = Customer.builder()
                .id(customerId)
                .name("Carlos Mendes")
                .phone("5593991112233")
                .email("carlos@provedor.com.br")
                .build();

        Invoice invDMinus3 = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .contractId(contractId)
                .amount(new BigDecimal("99.90"))
                .dueDate(refDate.plusDays(3))
                .status(Invoice.InvoiceStatus.PENDING)
                .pixCopiaECola("000201PIXD3")
                .build();

        Invoice invDZero = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .contractId(contractId)
                .amount(new BigDecimal("129.90"))
                .dueDate(refDate)
                .status(Invoice.InvoiceStatus.PENDING)
                .pixCopiaECola("000201PIXD0")
                .build();

        Invoice invDPlus5 = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .contractId(contractId)
                .amount(new BigDecimal("149.90"))
                .dueDate(refDate.minusDays(5))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .pixCopiaECola("000201PIXD5")
                .build();

        when(invoiceRepository.findAll()).thenReturn(List.of(invDMinus3, invDZero, invDPlus5));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(notificationConfigRepository.findByActiveTrue()).thenReturn(List.of(
                NotificationConfig.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .name("Gateway WhatsApp Principal")
                        .providerType(WhatsAppProviderType.EVOLUTION_API)
                        .active(true)
                        .build()
        ));
        when(dispatchRepository.existsByInvoiceIdAndStageAndChannel(any(), any(), any())).thenReturn(false);
        when(dispatchRepository.save(any(BillingNotificationDispatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BillingNotificationDunningService.SweepResult result =
                dunningService.runOmnichannelDunningNotifications(refDate);

        assertEquals(3, result.totalDispatched());
        assertEquals(1, result.dMinus3Count());
        assertEquals(1, result.dZeroCount());
        assertEquals(1, result.dPlus5AnatelCount());

        // Executa uma segunda vez com existsByInvoiceIdAndStageAndChannel = true para validar idempotência
        when(dispatchRepository.existsByInvoiceIdAndStageAndChannel(any(), any(), any())).thenReturn(true);
        BillingNotificationDunningService.SweepResult secondRun =
                dunningService.runOmnichannelDunningNotifications(refDate);

        assertEquals(0, secondRun.totalDispatched());
    }

    @Test
    void dispatchPaymentConfirmed_CreatesConfirmationRecordWhenPaid() {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        Invoice paidInvoice = Invoice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerId)
                .contractId(UuidCreatorUtils.generateUuidV7())
                .amount(new BigDecimal("99.90"))
                .status(Invoice.InvoiceStatus.PAID)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(
                Customer.builder().id(customerId).name("Ana Paula").phone("5593988887777").build()
        ));
        when(notificationConfigRepository.findByActiveTrue()).thenReturn(List.of());
        when(dispatchRepository.existsByInvoiceIdAndStageAndChannel(
                paidInvoice.getId(),
                BillingNotificationDispatch.Stage.PAYMENT_CONFIRMED,
                "WHATSAPP"
        )).thenReturn(false);
        when(dispatchRepository.save(any(BillingNotificationDispatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        dunningService.dispatchPaymentConfirmed(paidInvoice);

        verify(dispatchRepository, times(1)).save(argThat(d ->
                d.getStage() == BillingNotificationDispatch.Stage.PAYMENT_CONFIRMED
                        && "WHATSAPP".equals(d.getChannel())
                        && d.getMessagePreview().contains("Pagamento Confirmado!")
        ));
    }

    @Test
    void findActiveIncidentForContractEntity_CorrelatesByAffectedCtoId() {
        FtthIncidentRepository incidentRepo = mock(FtthIncidentRepository.class);
        OltPonPortRepository ponRepo = mock(OltPonPortRepository.class);
        NetworkDeviceRepository devRepo = mock(NetworkDeviceRepository.class);
        OnuProvisioningRepository onuRepo = mock(OnuProvisioningRepository.class);
        FtthCtoRepository ctoRepo = mock(FtthCtoRepository.class);
        FtthCtoPortRepository portRepo = mock(FtthCtoPortRepository.class);
        FtthCableRepository cableRepo = mock(FtthCableRepository.class);
        FtthMonitoringMapper mapper = mock(FtthMonitoringMapper.class);

        FtthCorrelationEngine engine = new FtthCorrelationEngine(
                incidentRepo,
                ponRepo,
                devRepo,
                onuRepo,
                ctoRepo,
                portRepo,
                cableRepo,
                mapper,
                new ObjectMapper()
        );

        UUID ctoId = UuidCreatorUtils.generateUuidV7();
        Contract contract = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .ctoId(ctoId)
                .build();

        FtthIncident activeIncident = FtthIncident.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .incidentType(IncidentType.FIBER_CUT_PROBABLE)
                .severity(IncidentSeverity.CRITICAL)
                .status(IncidentStatus.DISPATCHED)
                .title("Rompimento Troncal PON 0/1/2")
                .affectedCustomersCount(32)
                .affectedCtosIds("[\"" + ctoId + "\"]")
                .build();

        when(incidentRepo.findByStatusInOrderByDetectedAtDesc(any())).thenReturn(List.of(activeIncident));
        when(onuRepo.findByContractId(contract.getId())).thenReturn(Optional.empty());
        when(mapper.toIncidentResponse(activeIncident)).thenReturn(
                FtthIncidentResponse.builder()
                        .id(activeIncident.getId())
                        .title(activeIncident.getTitle())
                        .status(activeIncident.getStatus())
                        .severity(activeIncident.getSeverity())
                        .build()
        );

        Optional<FtthIncidentResponse> correlated = engine.findActiveIncidentForContractEntity(contract);

        assertTrue(correlated.isPresent());
        assertEquals("Rompimento Troncal PON 0/1/2", correlated.get().getTitle());
    }
}
