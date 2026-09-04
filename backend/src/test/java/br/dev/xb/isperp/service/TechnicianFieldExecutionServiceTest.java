package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.OltUnprovisionedOnuResponse;
import br.dev.xb.isperp.dto.TechnicianExecutionCompleteRequest;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TechnicianFieldExecutionServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InstallationMaterialDemandRepository demandRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private RadCheckRepository radCheckRepository;

    @Mock
    private RadAcctRepository radAcctRepository;

    @Mock
    private SerializedAssetRepository serializedAssetRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private TechnicianFieldExecutionService fieldService;

    private UUID workOrderId;
    private UUID contractId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        fieldService = new TechnicianFieldExecutionService(
                workOrderRepository,
                contractRepository,
                customerRepository,
                demandRepository,
                onuProvisioningRepository,
                radCheckRepository,
                radAcctRepository,
                serializedAssetRepository,
                domainEventPublisher
        );

        workOrderId = UuidCreatorUtils.generateUuidV7();
        contractId = UuidCreatorUtils.generateUuidV7();
        customerId = UuidCreatorUtils.generateUuidV7();
    }

    @Test
    @DisplayName("Deve descobrir ONUs associadas na O.S. sem inventar dados")
    void testListUnprovisionedOnus() {
        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .ctoPortNumber(2)
                .onuSerial("HWTC11223344")
                .onuMac("AA:BB:CC:DD:EE:01")
                .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));

        List<OltUnprovisionedOnuResponse> onus = fieldService.listUnprovisionedOnus(workOrderId);

        assertThat(onus).hasSize(1);
        assertThat(onus.get(0).getOnuSerial()).isEqualTo("HWTC11223344");
        assertThat(onus.get(0).getOnuMac()).isEqualTo("AA:BB:CC:DD:EE:01");
        assertThat(onus.get(0).getRxPowerDbm()).isNull();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando O.S. não tiver ONU associada")
    void testListUnprovisionedOnusWhenNoneAssigned() {
        WorkOrder wo = WorkOrder.builder()
                .id(workOrderId)
                .contractId(contractId)
                .customerId(customerId)
                .ctoPortNumber(2)
                .build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));

        List<OltUnprovisionedOnuResponse> onus = fieldService.listUnprovisionedOnus(workOrderId);

        assertThat(onus).isEmpty();
    }

    @Test
    @DisplayName("Deve provisionar ONU com VLAN e credenciais PPPoE")
    void testProvisionOnu() {
        WorkOrder wo = WorkOrder.builder().id(workOrderId).contractId(contractId).customerId(customerId).build();
        Contract contract = Contract.builder().id(contractId).customerId(customerId).build();
        Customer customer = Customer.builder().id(customerId).name("João Silva").build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.empty());
        when(radCheckRepository.findByUsernameAndAttribute(any(), any())).thenReturn(Optional.empty());

        Map<String, Object> result = fieldService.provisionOnu(workOrderId, "HWTC11223344", 100, "joaosilva", "senha123");

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("onuSerial")).isEqualTo("HWTC11223344");
        verify(domainEventPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("Deve concluir O.S. com evidências, potência dBm, assinatura e ativar o cliente")
    void testCompleteInstallation() {
        WorkOrder wo = WorkOrder.builder().id(workOrderId).contractId(contractId).customerId(customerId).build();
        Contract contract = Contract.builder().id(contractId).customerId(customerId).contractNumber("CTR-999").build();
        Customer customer = Customer.builder().id(customerId).name("João Silva").build();
        InstallationMaterialDemand demand = InstallationMaterialDemand.builder().build();

        when(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(i -> i.getArgument(0));
        when(demandRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(demand));

        TechnicianExecutionCompleteRequest req = TechnicianExecutionCompleteRequest.builder()
                .onuSerial("HWTC11223344")
                .fiberSignalDbm(BigDecimal.valueOf(-19.20))
                .installationPhotoUrl("https://s3.isperp.local/photos/inst-1.jpg")
                .digitalSignatureBase64("data:image/png;base64,iVBORw0KGgo...")
                .customerSignatureName("João Silva")
                .notes("Instalação realizada com sucesso, sinal perfeito.")
                .build();

        WorkOrder completed = fieldService.completeInstallation(workOrderId, req);

        assertThat(completed.getStatus()).isEqualTo(WorkOrder.WorkOrderStatus.COMPLETED);
        assertThat(completed.getFiberSignalDbm()).isEqualTo(BigDecimal.valueOf(-19.20));
        assertThat(contract.getStatus()).isEqualTo(Contract.ContractStatus.ACTIVE);
        verify(domainEventPublisher, times(2)).publish(any()); // WORK_ORDER_COMPLETED + CONTRACT_ACTIVATED
    }
}
