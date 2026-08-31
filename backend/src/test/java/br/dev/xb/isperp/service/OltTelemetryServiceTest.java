package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.OltPonPortRequest;
import br.dev.xb.isperp.dto.OltPonPortResponse;
import br.dev.xb.isperp.entity.OltPonPort;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.mapper.FtthMonitoringMapper;
import br.dev.xb.isperp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OltTelemetryServiceTest {

    @Mock
    private NetworkDeviceRepository networkDeviceRepository;

    @Mock
    private OltPonPortRepository oltPonPortRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private OnuTelemetryRecordRepository onuTelemetryRecordRepository;

    @Mock
    private FtthCableRepository cableRepository;

    private FtthMonitoringMapper mapper = Mappers.getMapper(FtthMonitoringMapper.class);

    private OltTelemetryService telemetryService;

    @BeforeEach
    void setUp() {
        telemetryService = new OltTelemetryService(
                networkDeviceRepository,
                oltPonPortRepository,
                onuProvisioningRepository,
                onuTelemetryRecordRepository,
                cableRepository,
                mapper
        );
    }

    @Test
    @DisplayName("Deve cadastrar porta PON e calcular métricas")
    void testCreatePonPort() {
        UUID deviceId = UUID.randomUUID();
        OltPonPortRequest request = OltPonPortRequest.builder()
                .networkDeviceId(deviceId)
                .slotNumber(0)
                .portNumber(1)
                .ponName("GPON 0/1/1")
                .build();

        OltPonPort saved = OltPonPort.builder()
                .id(UUID.randomUUID())
                .networkDeviceId(deviceId)
                .slotNumber(0)
                .portNumber(1)
                .ponName("GPON 0/1/1")
                .totalOnus(32)
                .onlineOnus(30)
                .build();

        when(oltPonPortRepository.save(any(OltPonPort.class))).thenReturn(saved);

        OltPonPortResponse response = telemetryService.createPonPort(request);

        assertThat(response).isNotNull();
        assertThat(response.getPonName()).isEqualTo("GPON 0/1/1");
        assertThat(response.getHealthPercentage()).isGreaterThan(90.0);
    }

    @Test
    @DisplayName("Deve atualizar resumo da PON a partir das ONUs com detecção de LOS")
    void testUpdatePonPortSummaryFromOnus() {
        UUID deviceId = UUID.randomUUID();
        OltPonPort port = OltPonPort.builder()
                .id(UUID.randomUUID())
                .networkDeviceId(deviceId)
                .ponName("GPON 0/1/1")
                .build();

        OnuProvisioning onu1 = OnuProvisioning.builder()
                .id(UUID.randomUUID())
                .networkDeviceId(deviceId)
                .rxPowerDbm(new BigDecimal("-19.50")) // Online
                .build();

        OnuProvisioning onu2 = OnuProvisioning.builder()
                .id(UUID.randomUUID())
                .networkDeviceId(deviceId)
                .rxPowerDbm(new BigDecimal("-45.00")) // LOS
                .build();

        when(onuProvisioningRepository.findByNetworkDeviceId(deviceId)).thenReturn(List.of(onu1, onu2));
        when(oltPonPortRepository.save(any(OltPonPort.class))).thenAnswer(i -> i.getArgument(0));

        telemetryService.updatePonPortSummaryFromOnus(port);

        assertThat(port.getTotalOnus()).isEqualTo(2);
        assertThat(port.getOnlineOnus()).isEqualTo(1);
        assertThat(port.getLosOnus()).isEqualTo(1);
        assertThat(port.getOperStatus()).isEqualTo("DEGRADED");
    }
}
