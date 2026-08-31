package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.FtthIncidentResponse;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.FtthMonitoringMapper;
import br.dev.xb.isperp.monitoring.IncidentSeverity;
import br.dev.xb.isperp.monitoring.IncidentType;
import br.dev.xb.isperp.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FtthCorrelationEngineTest {

    @Mock
    private FtthIncidentRepository incidentRepository;

    @Mock
    private OltPonPortRepository oltPonPortRepository;

    @Mock
    private NetworkDeviceRepository networkDeviceRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private FtthCtoRepository ctoRepository;

    @Mock
    private FtthCtoPortRepository ctoPortRepository;

    @Mock
    private FtthCableRepository cableRepository;

    private FtthMonitoringMapper mapper = Mappers.getMapper(FtthMonitoringMapper.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    private FtthCorrelationEngine correlationEngine;

    @BeforeEach
    void setUp() {
        correlationEngine = new FtthCorrelationEngine(
                incidentRepository,
                oltPonPortRepository,
                networkDeviceRepository,
                onuProvisioningRepository,
                ctoRepository,
                ctoPortRepository,
                cableRepository,
                mapper,
                objectMapper
        );
    }

    @Test
    @DisplayName("Deve correlacionar queda em massa de CTOs e classificar como Rompimento Troncal Provável")
    void testCorrelateMassiveLosAsFiberCut() {
        UUID deviceId = UUID.randomUUID();
        UUID ponId = UUID.randomUUID();
        UUID cto1Id = UUID.randomUUID();
        UUID cto2Id = UUID.randomUUID();

        OltPonPort pon = OltPonPort.builder()
                .id(ponId)
                .networkDeviceId(deviceId)
                .ponName("GPON 0/1/1")
                .totalOnus(10)
                .losOnus(8)
                .build();

        UUID onu1Id = UUID.randomUUID();
        UUID onu2Id = UUID.randomUUID();
        UUID onu3Id = UUID.randomUUID();
        UUID onu4Id = UUID.randomUUID();

        OnuProvisioning onu1 = OnuProvisioning.builder().id(onu1Id).networkDeviceId(deviceId).rxPowerDbm(new BigDecimal("-45.00")).build();
        OnuProvisioning onu2 = OnuProvisioning.builder().id(onu2Id).networkDeviceId(deviceId).rxPowerDbm(new BigDecimal("-45.00")).build();
        OnuProvisioning onu3 = OnuProvisioning.builder().id(onu3Id).networkDeviceId(deviceId).rxPowerDbm(new BigDecimal("-45.00")).build();
        OnuProvisioning onu4 = OnuProvisioning.builder().id(onu4Id).networkDeviceId(deviceId).rxPowerDbm(new BigDecimal("-45.00")).build();

        FtthCtoPort port1 = FtthCtoPort.builder().id(UUID.randomUUID()).ctoId(cto1Id).onuProvisioningId(onu1Id).build();
        FtthCtoPort port2 = FtthCtoPort.builder().id(UUID.randomUUID()).ctoId(cto1Id).onuProvisioningId(onu2Id).build();
        FtthCtoPort port3 = FtthCtoPort.builder().id(UUID.randomUUID()).ctoId(cto2Id).onuProvisioningId(onu3Id).build();
        FtthCtoPort port4 = FtthCtoPort.builder().id(UUID.randomUUID()).ctoId(cto2Id).onuProvisioningId(onu4Id).build();

        when(oltPonPortRepository.findAll()).thenReturn(List.of(pon));
        when(onuProvisioningRepository.findByNetworkDeviceId(deviceId)).thenReturn(List.of(onu1, onu2, onu3, onu4));

        when(ctoPortRepository.findByOnuProvisioningId(onu1Id)).thenReturn(Optional.of(port1));
        when(ctoPortRepository.findByOnuProvisioningId(onu2Id)).thenReturn(Optional.of(port2));
        when(ctoPortRepository.findByOnuProvisioningId(onu3Id)).thenReturn(Optional.of(port3));
        when(ctoPortRepository.findByOnuProvisioningId(onu4Id)).thenReturn(Optional.of(port4));

        when(ctoRepository.findById(cto1Id)).thenReturn(Optional.of(FtthCto.builder().id(cto1Id).latitude(new BigDecimal("-23.550000")).longitude(new BigDecimal("-46.630000")).build()));
        when(ctoRepository.findById(cto2Id)).thenReturn(Optional.of(FtthCto.builder().id(cto2Id).latitude(new BigDecimal("-23.551000")).longitude(new BigDecimal("-46.631000")).build()));

        when(incidentRepository.findAll()).thenReturn(List.of());
        when(incidentRepository.save(any(FtthIncident.class))).thenAnswer(i -> i.getArgument(0));

        List<FtthIncidentResponse> result = correlationEngine.runCorrelationAnalysis();

        assertThat(result).hasSize(1);
        FtthIncidentResponse incident = result.get(0);
        assertThat(incident.getIncidentType()).isEqualTo(IncidentType.FIBER_CUT_PROBABLE);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(incident.getAffectedCustomersCount()).isEqualTo(4);
        assertThat(incident.getEstimatedCutLatitude()).isNotNull();
    }
}
