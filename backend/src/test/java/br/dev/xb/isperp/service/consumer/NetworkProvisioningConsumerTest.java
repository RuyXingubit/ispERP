package br.dev.xb.isperp.service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.service.ContractService;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.NetworkProvisioningService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkProvisioningConsumerTest {

    @Mock
    private NetworkProvisioningService provisioningService;

    @Mock
    private ContractService contractService;

    @Mock
    private IdempotencyService idempotencyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NetworkProvisioningConsumer consumer;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());
    }

    @Test
    @DisplayName("Deve provisionar ONU automaticamente ao receber evento WORK_ORDER_COMPLETED")
    void shouldProvisionOnuOnWorkOrderCompleted() {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        Contract sampleContract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-001")
                .build();

        when(contractService.getContractById(contractId)).thenReturn(Optional.of(sampleContract));

        Map<String, Object> payload = Map.of(
                "contractId", contractId.toString(),
                "onuMac", "AA:BB:CC:DD:EE:01",
                "onuSerial", "HWTC12345678",
                "opticalSignalDbm", "-19.50"
        );

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("WORK_ORDER_COMPLETED")
                .payload(payload)
                .build();

        consumer.handleDomainEvent(event);

        verify(provisioningService, times(1)).provisionOnuForContract(
                sampleContract, "AA:BB:CC:DD:EE:01", "HWTC12345678", new BigDecimal("-19.50")
        );
    }

    @Test
    @DisplayName("Deve desbloquear internet do cliente ao receber evento INVOICE_PAID")
    void shouldUnblockInternetOnInvoicePaid() {
        UUID contractId = UuidCreatorUtils.generateUuidV7();

        Map<String, Object> payload = Map.of(
                "contractId", contractId.toString(),
                "amount", "119.90"
        );

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("INVOICE_PAID")
                .payload(payload)
                .build();

        consumer.handleDomainEvent(event);

        verify(provisioningService, times(1)).unblockInternetAccess(contractId);
    }
}
