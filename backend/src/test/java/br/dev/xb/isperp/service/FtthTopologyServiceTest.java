package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.ftth.FtthPortStatus;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtthTopologyServiceTest {

    @Mock
    private FtthPopRepository popRepository;

    @Mock
    private FtthPoleRepository poleRepository;

    @Mock
    private FtthCableRepository cableRepository;

    @Mock
    private FtthClosureRepository closureRepository;

    @Mock
    private FtthSplitterRepository splitterRepository;

    @Mock
    private FtthCtoRepository ctoRepository;

    @Mock
    private FtthCtoPortRepository ctoPortRepository;

    @Mock
    private FtthFusionRepository fusionRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private CustomerRepository customerRepository;

    private FtthColorService colorService = new FtthColorService();
    private FtthMapper mapper = Mappers.getMapper(FtthMapper.class);

    private FtthTopologyService topologyService;

    @BeforeEach
    void setUp() {
        topologyService = new FtthTopologyService(
                popRepository,
                poleRepository,
                cableRepository,
                closureRepository,
                splitterRepository,
                ctoRepository,
                ctoPortRepository,
                fusionRepository,
                onuProvisioningRepository,
                customerRepository,
                colorService,
                mapper
        );
    }

    @Test
    @DisplayName("Deve criar CTO e gerar automaticamente as portas 1 a 16")
    void testCreateCtoGeneratesPorts() {
        UUID ctoId = UUID.randomUUID();
        FtthCtoRequest request = FtthCtoRequest.builder()
                .name("CTO-CENTRO-01")
                .latitude(new BigDecimal("-23.550520"))
                .longitude(new BigDecimal("-46.633308"))
                .totalPorts(16)
                .splitterType("BALANCED_1_16")
                .status("ATIVA")
                .build();

        FtthCto savedCto = FtthCto.builder()
                .id(ctoId)
                .name("CTO-CENTRO-01")
                .latitude(new BigDecimal("-23.550520"))
                .longitude(new BigDecimal("-46.633308"))
                .totalPorts(16)
                .splitterType("BALANCED_1_16")
                .status("ATIVA")
                .build();

        when(ctoRepository.save(any(FtthCto.class))).thenReturn(savedCto);
        when(ctoPortRepository.findByCtoIdOrderByPortNumberAsc(ctoId)).thenReturn(List.of());

        FtthCtoResponse response = topologyService.createCto(request);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("CTO-CENTRO-01");
        assertThat(response.getTotalPorts()).isEqualTo(16);

        verify(ctoPortRepository).saveAll(argThat(list -> ((List<?>) list).size() == 16));
    }

    @Test
    @DisplayName("Deve calcular viabilidade de vendas identificando CTOs próximas com portas livres")
    void testCalculateFeasibilityFindsNearbyCtos() {
        UUID ctoId = UUID.randomUUID();
        FtthCto cto = FtthCto.builder()
                .id(ctoId)
                .name("CTO-CENTRO-01")
                .latitude(new BigDecimal("-23.550520"))
                .longitude(new BigDecimal("-46.633308"))
                .totalPorts(16)
                .status("ATIVA")
                .build();

        when(ctoRepository.findAll()).thenReturn(List.of(cto));
        when(ctoPortRepository.countByCtoIdAndStatus(ctoId, FtthPortStatus.LIVRE)).thenReturn(5L);
        when(ctoPortRepository.findByCtoIdOrderByPortNumberAsc(ctoId)).thenReturn(List.of());

        FtthFeasibilityRequest request = FtthFeasibilityRequest.builder()
                .latitude(new BigDecimal("-23.550550")) // A ~15 metros de distância
                .longitude(new BigDecimal("-46.633320"))
                .maxDistanceMeters(200.0)
                .build();

        FtthFeasibilityResponse response = topologyService.calculateFeasibility(request);

        assertThat(response.isViable()).isTrue();
        assertThat(response.getViableCtosCount()).isEqualTo(1);
        assertThat(response.getNearbyCtos()).hasSize(1);
        assertThat(response.getNearbyCtos().get(0).getFreePorts()).isEqualTo(5);
        assertThat(response.getNearbyCtos().get(0).getDistanceMeters()).isLessThan(50.0);
    }
}
