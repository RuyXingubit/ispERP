package br.dev.xb.isperp.service.consumer;

import br.dev.xb.isperp.dto.RadiusPolicyConfigResponse;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.radius.RadiusLifecycleActionType;
import br.dev.xb.isperp.service.IdempotencyService;
import br.dev.xb.isperp.service.RadiusLifecycleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RadiusLifecycleEventListenerTest {

    @Mock
    private RadiusLifecycleService radiusLifecycleService;

    @Mock
    private IdempotencyService idempotencyService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RadiusLifecycleEventListener eventListener;

    private UUID contractId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        // Faz o mock do IdempotencyService executar o Runnable diretamente
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());
    }

    @Test
    @DisplayName("Deve desbloquear cliente no RADIUS ao receber evento INVOICE_PAID quando elegível")
    void shouldExecuteInstantUnblockOnInvoicePaidWhenEligible() {
        when(radiusLifecycleService.getPolicyConfigResponse()).thenReturn(
                RadiusPolicyConfigResponse.builder()
                        .unblockOnPayment(true)
                        .build()
        );
        when(radiusLifecycleService.isCustomerEligibleForUnblock(customerId)).thenReturn(true);

        Map<String, Object> payload = Map.of(
                "invoiceId", UUID.randomUUID().toString(),
                "contractId", contractId.toString(),
                "customerId", customerId.toString(),
                "paymentMethod", "PIX"
        );

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("INVOICE_PAID")
                .aggregateType("Invoice")
                .aggregateId(UUID.randomUUID().toString())
                .payload(payload)
                .occurredAt(LocalDateTime.now())
                .build();

        eventListener.handleDomainEvents(event);

        verify(radiusLifecycleService).executeInstantUnblock(
                eq(contractId),
                contains("Pagamento compensado com sucesso (PIX)"),
                eq(RadiusLifecycleActionType.PAYMENT_UNBLOCK)
        );
    }

    @Test
    @DisplayName("Não deve desbloquear no RADIUS se o cliente ainda tiver outras faturas vencidas além da tolerância")
    void shouldNotUnblockIfCustomerHasRemainingOverdueInvoices() {
        when(radiusLifecycleService.getPolicyConfigResponse()).thenReturn(
                RadiusPolicyConfigResponse.builder()
                        .unblockOnPayment(true)
                        .build()
        );
        when(radiusLifecycleService.isCustomerEligibleForUnblock(customerId)).thenReturn(false);

        Map<String, Object> payload = Map.of(
                "contractId", contractId.toString(),
                "customerId", customerId.toString(),
                "paymentMethod", "PIX"
        );

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("INVOICE_PAID")
                .aggregateType("Invoice")
                .aggregateId(UUID.randomUUID().toString())
                .payload(payload)
                .occurredAt(LocalDateTime.now())
                .build();

        eventListener.handleDomainEvents(event);

        verify(radiusLifecycleService, never()).executeInstantUnblock(any(), any(), any());
    }

    @Test
    @DisplayName("Não deve desbloquear no RADIUS se a política unblockOnPayment estiver desativada")
    void shouldNotUnblockIfPolicyUnblockOnPaymentIsDisabled() {
        when(radiusLifecycleService.getPolicyConfigResponse()).thenReturn(
                RadiusPolicyConfigResponse.builder()
                        .unblockOnPayment(false)
                        .build()
        );

        Map<String, Object> payload = Map.of(
                "contractId", contractId.toString(),
                "customerId", customerId.toString(),
                "paymentMethod", "PIX"
        );

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("INVOICE_PAID")
                .payload(payload)
                .build();

        eventListener.handleDomainEvents(event);

        verify(radiusLifecycleService, never()).isCustomerEligibleForUnblock(any());
        verify(radiusLifecycleService, never()).executeInstantUnblock(any(), any(), any());
    }

    @Test
    @DisplayName("Deve executar desbloqueio em confiança ao receber evento CONTRACT_ACCESS_RESTORE_REQUESTED")
    void shouldExecuteTrustUnblockOnRestoreRequested() {
        Map<String, Object> payload = Map.of(
                "contractId", contractId.toString(),
                "unblockType", "APP_CENTRAL_48H"
        );

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("CONTRACT_ACCESS_RESTORE_REQUESTED")
                .aggregateType("Contract")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();

        eventListener.handleDomainEvents(event);

        verify(radiusLifecycleService).executeInstantUnblock(
                eq(contractId),
                contains("Desbloqueio em Confiança (APP_CENTRAL_48H)"),
                eq(RadiusLifecycleActionType.TRUST_UNBLOCK)
        );
    }

    @Test
    @DisplayName("Deve sincronizar credenciais no FreeRADIUS ao receber evento ONU_PROVISIONED")
    void shouldSyncCredentialsToRadiusOnOnuProvisioned() {
        Map<String, Object> payload = Map.of(
                "contractId", contractId.toString(),
                "serialNumber", "HWTC12345678"
        );

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("ONU_PROVISIONED")
                .aggregateType("OnuProvisioning")
                .aggregateId(contractId.toString())
                .payload(payload)
                .build();

        eventListener.handleDomainEvents(event);

        verify(radiusLifecycleService).syncContractToRadius(eq(contractId));
    }
}
