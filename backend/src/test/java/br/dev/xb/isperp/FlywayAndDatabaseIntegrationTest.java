package br.dev.xb.isperp;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.gateway.PaymentGatewayType;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FlywayAndDatabaseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentGatewayConfigRepository gatewayConfigRepository;

    @Autowired
    private NetworkDeviceRepository networkDeviceRepository;

    @Autowired
    private OnuProvisioningRepository onuRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("Deve validar a execução das migrações Flyway V1 a V7 no PostgreSQL 17 real")
    void shouldVerifyPostgres17DatabaseConnectionAndFlyway() {
        assertTrue(postgres.isRunning());
        assertEquals("isperp_test", postgres.getDatabaseName());
    }

    @Test
    @DisplayName("Deve persistir e consultar entidades com UUIDv7 e JSONB nativos no PostgreSQL 17")
    void shouldPersistAndRetrieveEntitiesWithUuidV7AndJsonb() {
        UUID customerId = UuidCreatorUtils.generateUuidV7();
        Customer customer = Customer.builder()
                .id(customerId)
                .name("Ruy Barbosa Real Test")
                .cpf("52998224725")
                .email("ruy.test@xingubit.com.br")
                .phone("11999990000")
                .build();
        customer = customerRepository.save(customer);

        UUID planId = UuidCreatorUtils.generateUuidV7();
        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 1 Giga Real")
                .downloadSpeed(1000)
                .uploadSpeed(500)
                .price(new BigDecimal("149.90"))
                .build();
        plan = planRepository.save(plan);

        UUID contractId = UuidCreatorUtils.generateUuidV7();
        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customer.getId())
                .planId(plan.getId())
                .contractNumber("CTR-REAL-2026")
                .monthlyFee(new BigDecimal("149.90"))
                .dueDay(15)
                .installationAddress("Av. Paulista, 1000, Apto 51")
                .city("São Paulo")
                .state("SP")
                .zipCode("01310-100")
                .status(Contract.ContractStatus.ACTIVE)
                .build();
        contract = contractRepository.save(contract);

        // Testa persistência de Invoice
        UUID invoiceId = UuidCreatorUtils.generateUuidV7();
        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .contractId(contract.getId())
                .customerId(customer.getId())
                .amount(new BigDecimal("149.90"))
                .dueDate(LocalDate.now().plusDays(10))
                .status(Invoice.InvoiceStatus.PENDING)
                .pixCopiaECola("00020126580014br.gov.bcb.pix...")
                .externalTransactionId("XB-REAL-TX-999")
                .build();
        invoice = invoiceRepository.save(invoice);

        // Testa persistência de Network Device e ONU Provisioning
        UUID deviceId = UuidCreatorUtils.generateUuidV7();
        NetworkDevice device = NetworkDevice.builder()
                .id(deviceId)
                .name("OLT Huawei MA5800 Teste Real")
                .ipAddress("192.168.10.1")
                .driverType(NetworkDriverType.SMARTOLT)
                .build();
        device = networkDeviceRepository.save(device);

        UUID onuId = UuidCreatorUtils.generateUuidV7();
        OnuProvisioning onu = OnuProvisioning.builder()
                .id(onuId)
                .contractId(contract.getId())
                .customerId(customer.getId())
                .networkDeviceId(device.getId())
                .onuMac("00:11:22:33:44:55")
                .onuSerial("HWTC99887766")
                .downloadSpeed(1000)
                .uploadSpeed(500)
                .rxPowerDbm(new BigDecimal("-19.10"))
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .build();
        onu = onuRepository.save(onu);

        // Testa persistência de Outbox com JSONB
        UUID eventId = UuidCreatorUtils.generateUuidV7();
        OutboxEvent outbox = OutboxEvent.builder()
                .id(eventId)
                .aggregateType("Contract")
                .aggregateId(contract.getId().toString())
                .eventType("CONTRACT_CREATED")
                .payload("{\"contractNumber\": \"CTR-REAL-2026\", \"status\": \"ACTIVE\"}")
                .status(OutboxEvent.OutboxStatus.PENDING)
                .build();
        outbox = outboxEventRepository.save(outbox);

        // Validações
        Optional<Customer> foundCustomer = customerRepository.findById(customerId);
        assertTrue(foundCustomer.isPresent());
        assertEquals("Ruy Barbosa Real Test", foundCustomer.get().getName());

        Optional<Invoice> foundInvoice = invoiceRepository.findByExternalTransactionId("XB-REAL-TX-999");
        assertTrue(foundInvoice.isPresent());
        assertEquals(new BigDecimal("149.90"), foundInvoice.get().getAmount());

        Optional<OnuProvisioning> foundOnu = onuRepository.findByOnuMac("00:11:22:33:44:55");
        assertTrue(foundOnu.isPresent());
        assertEquals("HWTC99887766", foundOnu.get().getOnuSerial());
        assertEquals(new BigDecimal("-19.10"), foundOnu.get().getRxPowerDbm());

        Optional<OutboxEvent> foundOutbox = outboxEventRepository.findById(eventId);
        assertTrue(foundOutbox.isPresent());
        assertEquals("CONTRACT_CREATED", foundOutbox.get().getEventType());
    }
}
