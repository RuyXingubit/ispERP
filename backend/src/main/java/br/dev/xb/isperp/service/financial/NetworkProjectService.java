package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.NetworkProjectPaybackDto;
import br.dev.xb.isperp.dto.financial.NetworkProjectRequest;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.FtthCto;
import br.dev.xb.isperp.entity.financial.NetworkProject;
import br.dev.xb.isperp.entity.financial.ProjectStatus;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.ftth.FtthPortStatus;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.FtthCtoPortRepository;
import br.dev.xb.isperp.repository.FtthCtoRepository;
import br.dev.xb.isperp.repository.financial.NetworkProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkProjectService {

    private final NetworkProjectRepository projectRepository;
    private final FtthCtoRepository ctoRepository;
    private final FtthCtoPortRepository ctoPortRepository;
    private final ContractRepository contractRepository;

    private static final BigDecimal AVERAGE_TICKET_FALLBACK = new BigDecimal("99.90");

    @Transactional
    public NetworkProject createProject(NetworkProjectRequest request) {
        NetworkProject project = NetworkProject.builder()
                .name(request.getName())
                .neighborhood(request.getNeighborhood())
                .city(request.getCity())
                .budgetAmount(request.getBudgetAmount())
                .targetSubscribers(request.getTargetSubscribers())
                .startDate(request.getStartDate() != null ? request.getStartDate() : java.time.LocalDate.now())
                .status(ProjectStatus.ACTIVE)
                .notes(request.getNotes())
                .build();

        NetworkProject saved = projectRepository.save(project);
        log.info("Projeto de Rede criado: {} no bairro {}, Orçamento: R$ {}",
                saved.getName(), saved.getNeighborhood(), saved.getBudgetAmount());
        return saved;
    }

    @Transactional
    public void assignCtoToProject(UUID ctoId, UUID projectId) {
        FtthCto cto = ctoRepository.findById(ctoId)
                .orElseThrow(() -> new ResourceNotFoundException("CTO não encontrada: " + ctoId));

        cto.setProjectId(projectId);
        ctoRepository.save(cto);
        log.info("CTO {} associada ao Projeto de Rede {}", cto.getName(), projectId);
    }

    @Transactional(readOnly = true)
    public List<NetworkProjectPaybackDto> getAllProjectsWithPayback() {
        List<NetworkProject> projects = projectRepository.findAll();
        List<NetworkProjectPaybackDto> results = new ArrayList<>();

        // Ticket médio geral dos contratos ativos para precisão
        List<Contract> activeContracts = contractRepository.findByStatus(Contract.ContractStatus.ACTIVE);
        BigDecimal averageTicket = AVERAGE_TICKET_FALLBACK;
        if (!activeContracts.isEmpty()) {
            BigDecimal sumFees = activeContracts.stream()
                    .map(c -> c.getMonthlyFee() != null ? c.getMonthlyFee() : AVERAGE_TICKET_FALLBACK)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageTicket = sumFees.divide(BigDecimal.valueOf(activeContracts.size()), 2, RoundingMode.HALF_UP);
        }

        for (NetworkProject proj : projects) {
            List<FtthCto> projectCtos = ctoRepository.findByProjectId(proj.getId());

            int totalPorts = 0;
            int occupiedPorts = 0;

            for (FtthCto cto : projectCtos) {
                totalPorts += cto.getTotalPorts();
                occupiedPorts += (int) ctoPortRepository.countByCtoIdAndStatus(cto.getId(), FtthPortStatus.OCUPADA);
            }

            // Taxa de ocupação física
            BigDecimal occupancyRate = BigDecimal.ZERO;
            if (totalPorts > 0) {
                occupancyRate = BigDecimal.valueOf(occupiedPorts)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalPorts), 1, RoundingMode.HALF_UP);
            }

            // MRR Gerado no Bairro: Portas Ocupadas x Ticket Médio
            BigDecimal generatedMrr = averageTicket.multiply(BigDecimal.valueOf(occupiedPorts));

            // Margem Líquida Mensal estimada de Telecom (MRR - 20% de impostos/banda)
            BigDecimal netMonthlyContribution = generatedMrr.multiply(new BigDecimal("0.80")).setScale(2, RoundingMode.HALF_UP);

            // Payback Acumulado em Meses: Orçamento / Margem Líquida Mensal
            BigDecimal paybackMonths = BigDecimal.valueOf(999.0);
            boolean paybackReached = false;

            if (netMonthlyContribution.compareTo(BigDecimal.ZERO) > 0) {
                paybackMonths = proj.getBudgetAmount().divide(netMonthlyContribution, 1, RoundingMode.HALF_UP);
                if (paybackMonths.compareTo(BigDecimal.valueOf(12.0)) <= 0 && occupiedPorts >= (proj.getTargetSubscribers() * 0.7)) {
                    paybackReached = true;
                }
            }

            // O Direcionador Comercial do Dono (Termômetro)
            String alert;
            String priorityLevel;

            if (occupancyRate.compareTo(new BigDecimal("25.0")) < 0) {
                alert = String.format("🚨 DINHEIRO DORMINDO NO POSTE: O bairro %s possui apenas %.1f%% de ocupação (%d/%d portas). Concentre panfletagem e vendas aqui para acelerar o retorno do capital investido.",
                        proj.getNeighborhood(), occupancyRate, occupiedPorts, totalPorts);
                priorityLevel = "IDLE_NETWORK_FOCUS";
            } else if (occupancyRate.compareTo(new BigDecimal("70.0")) >= 0) {
                alert = String.format("🚀 ESGOTAMENTO IMINENTE: O bairro %s atingiu %.1f%% de ocupação. Planeje expansão de novas CTOs para não perder vendas.",
                        proj.getNeighborhood(), occupancyRate);
                priorityLevel = "HIGH_RETURN";
            } else {
                alert = String.format("⚖️ EXPANSÃO SAUDÁVEL: O projeto no bairro %s caminha para o payback em %.1f meses.",
                        proj.getNeighborhood(), paybackMonths);
                priorityLevel = "NORMAL";
            }

            results.add(NetworkProjectPaybackDto.builder()
                    .projectId(proj.getId())
                    .name(proj.getName())
                    .neighborhood(proj.getNeighborhood())
                    .city(proj.getCity())
                    .status(proj.getStatus())
                    .budgetAmount(proj.getBudgetAmount())
                    .targetSubscribers(proj.getTargetSubscribers())
                    .startDate(proj.getStartDate())
                    .ctoCount(projectCtos.size())
                    .totalPorts(totalPorts)
                    .occupiedPorts(occupiedPorts)
                    .occupancyRatePercentage(occupancyRate)
                    .activeSubscribers(occupiedPorts)
                    .generatedMrr(generatedMrr)
                    .monthlyNetContribution(netMonthlyContribution)
                    .accumulatedPaybackMonths(paybackMonths)
                    .isPaybackReached(paybackReached)
                    .commercialDirectionAlert(alert)
                    .priorityLevel(priorityLevel)
                    .build());
        }

        return results;
    }
}
