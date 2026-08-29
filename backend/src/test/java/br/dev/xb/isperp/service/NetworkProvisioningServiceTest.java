package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.network.NetworkDriverResolver;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.network.NetworkProvisioner;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.OnuProvisioningRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkProvisioningServiceTest {

    @Mock
    private OnuProvisioningRepository onuRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private NetworkDriverResolver driverResolver;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private NetworkProvisioner networkProvisioner;

    @InjectMocks
    private NetworkProvisioningService provisioningService;

    private Contract sampleContract;
    private Customer sampleCustomer;
    private Plan samplePlan;
    private NetworkDevice sampleDevice;
    private UUID contractId;
    private UUID customerId;
    private UUID planId;

    @BeforeEach
    void setUp() {
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
        planId = UuidCreatorUtils.generateUuidV7();

        sampleCustomer = Customer.builder()
                .id(customerId)
                .name("Ruy Barbosa")
                .cpf("12345678909")
                .build();

        samplePlan = Plan.builder()
                .id(planId)
                .name("Fibra 600 Mega")
                .downloadSpeed(600)
                .uploadSpeed(300)
                .build();

        sampleContract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .planId(planId)
                .contractNumber("CTR-2026001")
                .build();

        sampleDevice = NetworkDevice.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("SmartOLT Central")
                .driverType(NetworkDriverType.SMARTOLT)
                .build();
    }

    @Test
    @DisplayName("Deve provisionar ONU para o contrato e emitir evento ONU_PROVISIONED")
    void shouldProvisionOnuForContract() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(planRepository.findById(planId)).thenReturn(Optional.of(samplePlan));
        when(driverResolver.resolve(NetworkDriverType.SMARTOLT))
                .thenReturn(new NetworkDriverResolver.ResolvedNetworkDriver(networkProvisioner, sampleDevice));
        when(onuRepository.findByContractId(contractId)).thenReturn(Optional.empty());
        when(onuRepository.save(any(OnuProvisioning.class))).thenAnswer(i -> i.getArgument(0));

        OnuProvisioning onu = provisioningService.provisionOnuForContract(
                sampleContract, "AA:BB:CC:DD:EE:01", "HWTC12345678", new BigDecimal("-19.20")
        );

        assertNotNull(onu);
        assertEquals("AA:BB:CC:DD:EE:01", onu.getOnuMac());
        assertEquals(OnuProvisioning.OnuStatus.PROVISIONED, onu.getStatus());
        assertEquals(600, onu.getDownloadSpeed());

        verify(networkProvisioner, times(1)).provisionOnu(any(), eq(sampleDevice));
        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve bloquear ONU por inadimplência e emitir evento INTERNET_ACCESS_BLOCKED")
    void shouldBlockInternetAccess() {
        OnuProvisioning activeOnu = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .onuMac("AA:BB:CC:DD:EE:01")
                .status(OnuProvisioning.OnuStatus.PROVISIONED)
                .build();

        when(onuRepository.findByContractId(contractId)).thenReturn(Optional.of(activeOnu));
        when(driverResolver.resolve(NetworkDriverType.SMARTOLT))
                .thenReturn(new NetworkDriverResolver.ResolvedNetworkDriver(networkProvisioner, sampleDevice));
        when(onuRepository.save(any(OnuProvisioning.class))).thenAnswer(i -> i.getArgument(0));

        OnuProvisioning blocked = provisioningService.blockInternetAccess(contractId, "Inadimplência de 15 dias");

        assertEquals(OnuProvisioning.OnuStatus.BLOCKED, blocked.getStatus());
        verify(networkProvisioner, times(1)).blockInternetAccess(eq("AA:BB:CC:DD:EE:01"), any(), eq(sampleDevice));
        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve desbloquear ONU após pagamento e emitir evento INTERNET_ACCESS_UNBLOCKED")
    void shouldUnblockInternetAccess() {
        OnuProvisioning blockedOnu = OnuProvisioning.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .onuMac("AA:BB:CC:DD:EE:01")
                .status(OnuProvisioning.OnuStatus.BLOCKED)
                .build();

        when(onuRepository.findByContractId(contractId)).thenReturn(Optional.of(blockedOnu));
        when(driverResolver.resolve(NetworkDriverType.SMARTOLT))
                .thenReturn(new NetworkDriverResolver.ResolvedNetworkDriver(networkProvisioner, sampleDevice));
        when(onuRepository.save(any(OnuProvisioning.class))).thenAnswer(i -> i.getArgument(0));

        OnuProvisioning unblocked = provisioningService.unblockInternetAccess(contractId);

        assertEquals(OnuProvisioning.OnuStatus.PROVISIONED, unblocked.getStatus());
        verify(networkProvisioner, times(1)).unblockInternetAccess(eq("AA:BB:CC:DD:EE:01"), eq(sampleDevice));
        verify(domainEventPublisher, times(1)).publish(any(DomainEvent.class));
    }
}
