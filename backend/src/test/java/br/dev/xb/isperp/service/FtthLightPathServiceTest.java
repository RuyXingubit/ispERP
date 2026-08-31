package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.ftth.FtthPortStatus;
import br.dev.xb.isperp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FtthLightPathServiceTest {

    @Mock
    private FtthCtoRepository ctoRepository;

    @Mock
    private FtthCtoPortRepository ctoPortRepository;

    @Mock
    private FtthFusionRepository fusionRepository;

    @Mock
    private FtthCableRepository cableRepository;

    @Mock
    private FtthSplitterRepository splitterRepository;

    @Mock
    private FtthClosureRepository closureRepository;

    @Mock
    private FtthPopRepository popRepository;

    private FtthLightPathService lightPathService;

    @BeforeEach
    void setUp() {
        lightPathService = new FtthLightPathService(
                ctoRepository,
                ctoPortRepository,
                fusionRepository,
                cableRepository,
                splitterRepository,
                closureRepository,
                popRepository
        );
    }

    @Test
    @DisplayName("Deve rastrear a rota óptica da porta da CTO até o POP e calcular a atenuação total")
    void testTraceLightPath() {
        UUID ctoId = UUID.randomUUID();
        UUID ctoPortId = UUID.randomUUID();
        UUID popId = UUID.randomUUID();
        UUID cableId = UUID.randomUUID();

        FtthCtoPort port = FtthCtoPort.builder()
                .id(ctoPortId)
                .ctoId(ctoId)
                .portNumber(1)
                .status(FtthPortStatus.OCUPADA)
                .build();

        FtthCto cto = FtthCto.builder()
                .id(ctoId)
                .name("CTO-01")
                .totalPorts(16)
                .splitterType("BALANCED_1_16")
                .build();

        FtthFusion fusion = FtthFusion.builder()
                .id(UUID.randomUUID())
                .sourceCableId(cableId)
                .sourceFiberNumber(1)
                .targetCtoId(ctoId)
                .lossDb(new BigDecimal("0.05"))
                .build();

        FtthCable cable = FtthCable.builder()
                .id(cableId)
                .name("CAB-ALIMENTADOR-72FO")
                .fiberCount(72)
                .lengthMeters(new BigDecimal("2000.00")) // 2km
                .attenuationDbPerKm(new BigDecimal("0.35")) // 0.70 dB
                .sourcePopId(popId)
                .build();

        FtthPop pop = FtthPop.builder()
                .id(popId)
                .name("POP 01 - Centro")
                .build();

        when(ctoPortRepository.findById(ctoPortId)).thenReturn(Optional.of(port));
        when(ctoRepository.findById(ctoId)).thenReturn(Optional.of(cto));
        when(fusionRepository.findAll()).thenReturn(List.of(fusion));
        when(cableRepository.findById(cableId)).thenReturn(Optional.of(cable));
        when(popRepository.findById(popId)).thenReturn(Optional.of(pop));

        FtthLightPathService.LightPathTraceResult result = lightPathService.traceLightPathFromCtoPort(ctoPortId);

        assertThat(result).isNotNull();
        assertThat(result.isReachedSource()).isTrue();
        assertThat(result.getSourcePopName()).isEqualTo("POP 01 - Centro");
        assertThat(result.getTotalAttenuationDb()).isGreaterThan(14.0); // 0.30 drop + 13.80 splitter 1:16 + 0.05 fusion + 0.70 cable
        assertThat(result.getNodes()).isNotEmpty();
    }
}
