package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.Tr069CommandLogResponse;
import br.dev.xb.isperp.api.dto.Tr069CpeStatusResponse;
import br.dev.xb.isperp.api.dto.UpdateTr069WifiRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.entity.Tr069CommandLog;
import br.dev.xb.isperp.entity.Tr069CpeConfig;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.OnuProvisioningRepository;
import br.dev.xb.isperp.repository.Tr069CommandLogRepository;
import br.dev.xb.isperp.repository.Tr069CpeConfigRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class Tr069CpeServiceTest {

    @Mock
    private Tr069CpeConfigRepository cpeConfigRepository;

    @Mock
    private Tr069CommandLogRepository commandLogRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    private Tr069CpeService service;

    @BeforeEach
    void setUp() {
        service = new Tr069CpeService(
                cpeConfigRepository,
                commandLogRepository,
                contractRepository,
                onuProvisioningRepository
        );
    }

    @Test
    @DisplayName("Onda M: Deve provisionar configuração TR-069 inicial vinculada à ONU do contrato")
    void shouldCreateInitialTr069ConfigForContract() {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        UUID onuId = UuidCreatorUtils.generateUuidV7();

        Contract contract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-2026-8841")
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        OnuProvisioning onu = OnuProvisioning.builder()
                .id(onuId)
                .contractId(contractId)
                .onuMac("HWTC8841AABB")
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(cpeConfigRepository.findByContractId(contractId)).thenReturn(Optional.empty());
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.of(onu));
        when(cpeConfigRepository.save(any(Tr069CpeConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandLogRepository.findTop15ByContractIdOrderByCreatedAtDesc(contractId)).thenReturn(List.of());

        Tr069CpeStatusResponse status = service.getCpeStatusByContract(contractId);

        assertThat(status.getContractId()).isEqualTo(contractId);
        assertThat(status.getOnuId()).isEqualTo(onuId);
        assertThat(status.getOnuSerial()).isEqualTo("HWTC8841AABB");
        assertThat(status.getSsid24Ghz()).isEqualTo("XinguFibra_8841_2G");
        assertThat(status.getSsid5Ghz()).isEqualTo("XinguFibra_8841_5G");
        assertThat(status.getAcsSyncStatus()).isEqualTo("SYNCED_EMBEDDED_ACS");
        assertThat(status.getConnectedHosts()).isEmpty();
    }

    @Test
    @DisplayName("Onda M: Deve atualizar SSID/Senha Dual-Band via TR-069 e registrar log de auditoria UUIDv7")
    void shouldUpdateWifiDualBandAndLogAudit() {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        Contract contract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-2026-9910")
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        Tr069CpeConfig existing = Tr069CpeConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .onuSerial("HWTC9910CCDD")
                .ssid24Ghz("XinguFibra_9910_2G")
                .password24Ghz("senhaantiga123")
                .channel24Ghz("AUTO")
                .ssid5Ghz("XinguFibra_9910_5G")
                .password5Ghz("senhaantiga123")
                .channel5Ghz("AUTO")
                .bandSteeringEnabled(true)
                .securityMode("WPA2-PSK/WPA3-SAE")
                .acsSyncStatus("SYNCED_EMBEDDED_ACS")
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(cpeConfigRepository.findByContractId(contractId)).thenReturn(Optional.of(existing));
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.empty());
        when(cpeConfigRepository.save(any(Tr069CpeConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandLogRepository.save(any(Tr069CommandLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandLogRepository.findTop15ByContractIdOrderByCreatedAtDesc(contractId)).thenReturn(List.of());

        UpdateTr069WifiRequest req = new UpdateTr069WifiRequest();
        req.setSsid24Ghz("Familia_Silva_2G");
        req.setPassword24Ghz("novaSenhaForte2026");
        req.setChannel24Ghz("6");
        req.setSsid5Ghz("Familia_Silva_5G");
        req.setPassword5Ghz("novaSenhaForte2026");
        req.setChannel5Ghz("36");
        req.setBandSteeringEnabled(false);

        Tr069CpeStatusResponse updated = service.updateWifiByContract(contractId, req, "SUBSCRIBER_PORTAL");

        assertThat(updated.getSsid24Ghz()).isEqualTo("Familia_Silva_2G");
        assertThat(updated.getChannel24Ghz()).isEqualTo("6");
        assertThat(updated.getSsid5Ghz()).isEqualTo("Familia_Silva_5G");
        assertThat(updated.getChannel5Ghz()).isEqualTo("36");
        assertThat(updated.getBandSteeringEnabled()).isFalse();
        verify(commandLogRepository).save(any(Tr069CommandLog.class));
    }

    @Test
    @DisplayName("Onda M: Deve rejeitar senha WPA2/WPA3 menor que 8 caracteres e disparar Reboot TR-069")
    void shouldRejectShortPasswordAndAllowReboot() {
        UUID contractId = UuidCreatorUtils.generateUuidV7();
        Contract contract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-2026-5001")
                .status(Contract.ContractStatus.ACTIVE)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        UpdateTr069WifiRequest invalidReq = new UpdateTr069WifiRequest();
        invalidReq.setSsid24Ghz("Rede_2G");
        invalidReq.setSsid5Ghz("Rede_5G");
        invalidReq.setPassword24Ghz("curta");

        assertThatThrownBy(() -> service.updateWifiByContract(contractId, invalidReq, "SUPPORT_AGENT"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 e 63 caracteres");

        Tr069CpeConfig existing = Tr069CpeConfig.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .onuSerial("HWTC5001")
                .ssid24Ghz("Rede_2G")
                .password24Ghz("senhaValida123")
                .ssid5Ghz("Rede_5G")
                .password5Ghz("senhaValida123")
                .uptimeSeconds(99999L)
                .build();

        when(cpeConfigRepository.findByContractId(contractId)).thenReturn(Optional.of(existing));
        when(onuProvisioningRepository.findByContractId(contractId)).thenReturn(Optional.empty());
        when(cpeConfigRepository.save(any(Tr069CpeConfig.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commandLogRepository.save(any(Tr069CommandLog.class))).thenAnswer(inv -> inv.getArgument(0));

        service.setGenieAcsUrl("http://127.0.0.1:1///");
        Tr069CommandLogResponse rebootLog = service.rebootCpeByContract(contractId, "SUPPORT_AGENT", "Analista N1");
        assertThat(rebootLog.getCommandType()).isEqualTo("REBOOT_CPE");
        assertThat(rebootLog.getOriginChannel()).isEqualTo("SUPPORT_AGENT");
        assertThat(existing.getUptimeSeconds()).isZero();
        assertThat(Tr069CpeService.stripTrailingSlashes("http://genieacs:7557///")).isEqualTo("http://genieacs:7557");
    }
}
