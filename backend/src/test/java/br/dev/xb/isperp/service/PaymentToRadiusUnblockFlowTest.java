package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.RadiusDisconnectRequest;
import br.dev.xb.isperp.dto.RadiusDisconnectResponse;
import br.dev.xb.isperp.dto.RadiusPolicyConfigResponse;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.gateway.PaymentGatewayResolver;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.gateway.xingubit.XingubitPayGateway;
import br.dev.xb.isperp.radius.NasVendorType;
import br.dev.xb.isperp.radius.RadiusLifecycleActionType;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.service.consumer.RadiusLifecycleEventListener;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Teste unitário e de fluxo que valida a esteira reativa completa de faturamento Pix,
 * notificação de Webhook, compensação e desbloqueio instantâneo RADIUS via CoA (PoD).
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PaymentToRadiusUnblockFlowTest {

    @Mock
    private PaymentGatewayResolver gatewayResolver;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Mock
    private RadiusPolicyConfigRepository policyConfigRepository;

    @Mock
    private RadiusLifecycleLogRepository lifecycleLogRepository;

    @Mock
    private RadiusProvisioningService radiusProvisioningService;

    @Mock
    private RadiusSessionService radiusSessionService;

    @Mock
    private NasRepository nasRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private NfcomDecisionService nfcomDecisionService;

    private XingubitPayGateway xingubitPayGateway;
    private ObjectMapper objectMapper;
    private DomainEventPublisher domainEventPublisher;
    private InvoiceService invoiceService;
    private PaymentWebhookService webhookService;
    private RadiusLifecycleService radiusLifecycleService;
    private RadiusLifecycleEventListener eventListener;

    private UUID customerId;
    private UUID contractId;
    private UUID planId;
    private UUID invoiceId;
    private String txId;
    private PaymentGatewayConfig gatewayConfig;
    private Customer customer;
    private Contract contract;
    private Plan plan;
    private OnuProvisioning onu;
    private Invoice invoice;
    private RadiusPolicyConfig policyConfig;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        xingubitPayGateway = new XingubitPayGateway();

        domainEventPublisher = new DomainEventPublisher(outboxEventRepository, applicationEventPublisher, objectMapper);

        invoiceService = new InvoiceService(
                invoiceRepository,
                customerRepository,
                transactionRepository,
                gatewayResolver,
                domainEventPublisher,
                auditLogService
        );

        webhookService = new PaymentWebhookService(
                gatewayResolver,
                invoiceService,
                transactionRepository,
                idempotencyService,
                objectMapper,
                nfcomDecisionService,
                xingubitPayGateway,
                customerRepository,
                invoiceRepository
        );

        var mapper = org.mapstruct.factory.Mappers.getMapper(br.dev.xb.isperp.mapper.RadiusLifecycleMapper.class);
        radiusLifecycleService = new RadiusLifecycleService(
                contractRepository,
                customerRepository,
                planRepository,
                onuProvisioningRepository,
                invoiceRepository,
                policyConfigRepository,
                lifecycleLogRepository,
                radiusProvisioningService,
                radiusSessionService,
                nasRepository,
                mapper
        );

        eventListener = new RadiusLifecycleEventListener(
                radiusLifecycleService,
                idempotencyService,
                objectMapper
        );

        // Dispara ações síncronas no mock do IdempotencyService
        lenient().doAnswer(inv -> {
            Runnable action = inv.getArgument(2);
            action.run();
            return true;
        }).when(idempotencyService).executeIdempotent(any(), any(), any());

        // Conecta o DomainEventPublisher diretamente ao RadiusLifecycleEventListener para o teste de fluxo
        lenient().doAnswer(inv -> {
            GenericDomainEvent event = inv.getArgument(0);
            eventListener.handleDomainEvents(event);
            return null;
        }).when(applicationEventPublisher).publishEvent(any(GenericDomainEvent.class));

        customerId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();
        invoiceId = UuidCreatorUtils.generateUuidV7();
        txId = "XB-TEST-COA-123456789";

        gatewayConfig = PaymentGatewayConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .gatewayType(PaymentGatewayType.XINGUBIT_PAY)
                .webhookSecret("segredo_webhook_teste_123")
                .pixKey("financeiro@isperp.com.br")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name("Carlos Eduardo Assinante")
                .cpf("12345678900")
                .email("carlos@provedor.local")
                .phone("93988887777")
                .build();

        plan = Plan.builder()
                .id(planId)
                .name("Fibra 600 Mega")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .price(new BigDecimal("119.90"))
                .build();

        // Contrato atualmente SUSPENSO / BLOQUEADO
        contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .planId(planId)
                .contractNumber("CTR-2026-999")
                .status(Contract.ContractStatus.SUSPENDED)
                .monthlyFee(new BigDecimal("119.90"))
                .build();

        onu = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .pppoeUser("carlos_pppoe")
                .pppoePassword("senhaPppoe123")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .status(OnuProvisioning.OnuStatus.BLOCKED)
                .fixedIp("100.64.10.50")
                .ipv6Prefix("2804:100:200::/64")
                .build();

        invoice = Invoice.builder()
                .id(invoiceId)
                .contractId(contractId)
                .customerId(customerId)
                .externalTransactionId(txId)
                .amount(new BigDecimal("119.90"))
                .dueDate(LocalDate.now().minusDays(3))
                .status(Invoice.InvoiceStatus.OVERDUE)
                .build();

        policyConfig = RadiusPolicyConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .autoBlockEnabled(true)
                .toleranceDays(5)
                .unblockOnPayment(true)
                .sendPodOnUnblock(true)
                .build();
    }

    @Test
    @DisplayName("Esteira Completa: Webhook Pix ➔ Compensação ➔ Evento INVOICE_PAID ➔ Desbloqueio radreply ➔ PoD Disconnect")
    void testEndToEndWebhookPaymentToRadiusUnblockAndPod() {
        // 1. Simulação do Gateway Resolver
        when(gatewayResolver.resolve(PaymentGatewayType.XINGUBIT_PAY))
                .thenReturn(new PaymentGatewayResolver.ResolvedGateway(xingubitPayGateway, gatewayConfig));

        // 2. Mocks dos Repositories para o fluxo de pagamento e faturas
        when(invoiceRepository.findByExternalTransactionId(txId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        // 3. Mocks dos Repositories para o fluxo de desbloqueio RADIUS
        when(policyConfigRepository.findFirstConfig()).thenReturn(Optional.of(policyConfig));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.of(onu));

        // Nenhuma outra fatura vencida além da tolerância
        when(invoiceRepository.findByCustomerIdAndStatus(customerId, Invoice.InvoiceStatus.PENDING))
                .thenReturn(List.of());

        // Simulação do envio de PoD com sucesso para o BNG
        when(radiusSessionService.disconnectUser(any(RadiusDisconnectRequest.class)))
                .thenReturn(RadiusDisconnectResponse.builder()
                        .success(true)
                        .message("Sessão desconectada via PoD no NAS 10.0.0.1")
                        .build());

        // 4. Montagem do Payload do Webhook Pix com Assinatura Criptográfica HMAC-SHA256
        Map<String, Object> webhookPayload = Map.of(
                "txid", txId,
                "paidAmount", "119.90",
                "status", "PAID"
        );
        String validHmacSignature = xingubitPayGateway.calculateSignature(
                webhookPayload.toString(),
                gatewayConfig.getWebhookSecret()
        );

        // 5. Execução do Webhook disparado pelo gateway bancário
        webhookService.processPaymentWebhook("XINGUBIT_PAY", webhookPayload, validHmacSignature);

        // 6. Asserções e Validações de Integridade da Esteira Completa:
        // A. Fatura marcada como PAGA
        assertThat(invoice.getStatus()).isEqualTo(Invoice.InvoiceStatus.PAID);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("119.90"));
        assertThat(invoice.getPaymentMethod()).isEqualTo("PIX");
        assertThat(invoice.getPaidAt()).isNotNull();

        // B. Contrato reativado para ACTIVE
        assertThat(contract.getStatus()).isEqualTo(Contract.ContractStatus.ACTIVE);
        verify(contractRepository).save(contract);

        // C. ONU reativada para PROVISIONED
        assertThat(onu.getStatus()).isEqualTo(OnuProvisioning.OnuStatus.PROVISIONED);
        verify(onuProvisioningRepository).save(onu);

        // D. Velocidade integral e atributos IPv6 restaurados no FreeRADIUS (radreply)
        verify(radiusProvisioningService).unblockUser(
                eq("carlos_pppoe"),
                eq(600L),
                eq(300L),
                eq(NasVendorType.MIKROTIK),
                eq("100.64.10.50"),
                eq("2804:100:200::/64"),
                isNull(),
                isNull()
        );

        // E. Pacote RFC 3576 Disconnect-Request (PoD) enviado para a porta UDP 3799 do NAS
        verify(radiusSessionService).disconnectUser(argThat(req ->
                "carlos_pppoe".equals(req.getUsername())
        ));

        // F. Auditoria persistida em radius_lifecycle_logs
        verify(lifecycleLogRepository).save(argThat(log ->
                contractId.equals(log.getContractId())
                && customerId.equals(log.getCustomerId())
                && "carlos_pppoe".equals(log.getUsername())
                && RadiusLifecycleActionType.PAYMENT_UNBLOCK.equals(log.getActionType())
                && log.isSuccess()
                && log.getDetails().contains("Sessão desconectada via PoD")
        ));
    }
}
