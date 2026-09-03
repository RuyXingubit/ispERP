package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.NetworkProjectPaybackDto;
import br.dev.xb.isperp.dto.financial.NetworkProjectRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.FtthCto;
import br.dev.xb.isperp.entity.financial.NetworkProject;
import br.dev.xb.isperp.entity.financial.ProjectStatus;
import br.dev.xb.isperp.ftth.FtthPortStatus;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.FtthCtoPortRepository;
import br.dev.xb.isperp.repository.FtthCtoRepository;
import br.dev.xb.isperp.repository.financial.NetworkProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetworkProjectServiceTest {

    @Mock
    private NetworkProjectRepository projectRepository;

    @Mock
    private FtthCtoRepository ctoRepository;

    @Mock
    private FtthCtoPortRepository ctoPortRepository;

    @Mock
    private ContractRepository contractRepository;

    private NetworkProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new NetworkProjectService(
                projectRepository,
                ctoRepository,
                ctoPortRepository,
                contractRepository
        );
    }

    @Test
    @DisplayName("Deve calcular Payback e acionar alerta de 'Dinheiro Dormindo no Poste' quando ocupação for baixa")
    void shouldTriggerIdleNetworkAlertWhenOccupancyIsLow() {
        UUID projId = UUID.randomUUID();
        NetworkProject proj = NetworkProject.builder()
                .id(projId)
                .name("Expansão Bairro Novo")
                .neighborhood("Bairro Novo")
                .city("Santarém")
                .budgetAmount(new BigDecimal("50000.00"))
                .targetSubscribers(100)
                .status(ProjectStatus.ACTIVE)
                .build();

        when(projectRepository.findAll()).thenReturn(List.of(proj));

        // 2 CTOs de 16 portas (Total 32 portas)
        UUID cto1Id = UUID.randomUUID();
        UUID cto2Id = UUID.randomUUID();
        FtthCto cto1 = FtthCto.builder().id(cto1Id).name("CTO-01").totalPorts(16).projectId(projId).build();
        FtthCto cto2 = FtthCto.builder().id(cto2Id).name("CTO-02").totalPorts(16).projectId(projId).build();

        when(ctoRepository.findByProjectId(projId)).thenReturn(List.of(cto1, cto2));

        // Apenas 2 portas ocupadas no total (ocupação de 6.25% < 25%)
        when(ctoPortRepository.countByCtoIdAndStatus(cto1Id, FtthPortStatus.OCUPADA)).thenReturn(1L);
        when(ctoPortRepository.countByCtoIdAndStatus(cto2Id, FtthPortStatus.OCUPADA)).thenReturn(1L);

        // Ticket médio R$ 100,00
        Contract c1 = Contract.builder().monthlyFee(new BigDecimal("100.00")).status(Contract.ContractStatus.ACTIVE).build();
        when(contractRepository.findByStatus(Contract.ContractStatus.ACTIVE)).thenReturn(List.of(c1));

        List<NetworkProjectPaybackDto> results = projectService.getAllProjectsWithPayback();

        assertThat(results).hasSize(1);
        NetworkProjectPaybackDto dto = results.get(0);
        assertThat(dto.getTotalPorts()).isEqualTo(32);
        assertThat(dto.getOccupiedPorts()).isEqualTo(2);
        assertThat(dto.getOccupancyRatePercentage()).isEqualByComparingTo(new BigDecimal("6.3"));
        assertThat(dto.getCommercialDirectionAlert()).contains("DINHEIRO DORMINDO NO POSTE");
        assertThat(dto.getPriorityLevel()).isEqualTo("IDLE_NETWORK_FOCUS");
    }
}
