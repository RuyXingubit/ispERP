package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.FtthMapper;
import br.dev.xb.isperp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtthFusionServiceTest {

    @Mock
    private FtthFusionRepository fusionRepository;

    @Mock
    private FtthCableRepository cableRepository;

    @Mock
    private FtthSplitterRepository splitterRepository;

    @Mock
    private FtthCtoRepository ctoRepository;

    @Mock
    private FtthTopologyService topologyService;

    private FtthColorService colorService = new FtthColorService();
    private FtthMapper mapper = Mappers.getMapper(FtthMapper.class);

    private FtthFusionService fusionService;

    @BeforeEach
    void setUp() {
        fusionService = new FtthFusionService(
                fusionRepository,
                cableRepository,
                splitterRepository,
                ctoRepository,
                topologyService,
                colorService,
                mapper
        );
    }

    @Test
    @DisplayName("Deve criar fusão com sucesso")
    void testCreateFusionSuccess() {
        UUID closureId = UUID.randomUUID();
        UUID sourceCableId = UUID.randomUUID();
        UUID targetCableId = UUID.randomUUID();

        FtthFusionRequest request = FtthFusionRequest.builder()
                .closureId(closureId)
                .sourceCableId(sourceCableId)
                .sourceFiberNumber(1)
                .targetCableId(targetCableId)
                .targetFiberNumber(1)
                .lossDb(new BigDecimal("0.02"))
                .build();

        FtthFusion saved = FtthFusion.builder()
                .id(UUID.randomUUID())
                .closureId(closureId)
                .sourceCableId(sourceCableId)
                .sourceFiberNumber(1)
                .targetCableId(targetCableId)
                .targetFiberNumber(1)
                .lossDb(new BigDecimal("0.02"))
                .build();

        when(fusionRepository.findBySourceCableIdAndSourceFiberNumber(sourceCableId, 1))
                .thenReturn(Optional.empty());
        when(fusionRepository.save(any(FtthFusion.class))).thenReturn(saved);

        FtthFusionResponse response = fusionService.createFusion(request);

        assertThat(response).isNotNull();
        assertThat(response.getClosureId()).isEqualTo(closureId);
        assertThat(response.getSourceFiberNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve rejeitar fusão se a fibra de origem já estiver fundida")
    void testCreateFusionRejectsDuplicateSourceFiber() {
        UUID closureId = UUID.randomUUID();
        UUID sourceCableId = UUID.randomUUID();

        FtthFusionRequest request = FtthFusionRequest.builder()
                .closureId(closureId)
                .sourceCableId(sourceCableId)
                .sourceFiberNumber(1)
                .build();

        FtthFusion existing = FtthFusion.builder()
                .id(UUID.randomUUID())
                .closureId(closureId)
                .sourceCableId(sourceCableId)
                .sourceFiberNumber(1)
                .build();

        when(fusionRepository.findBySourceCableIdAndSourceFiberNumber(sourceCableId, 1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> fusionService.createFusion(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já possui uma fusão cadastrada");
    }
}
