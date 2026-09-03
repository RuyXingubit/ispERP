package br.dev.xb.isperp;

import br.dev.xb.isperp.dto.financial.NetworkProjectPaybackDto;
import br.dev.xb.isperp.dto.financial.NetworkProjectRequest;
import br.dev.xb.isperp.dto.financial.SentinelAuditLogDto;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.FtthCto;
import br.dev.xb.isperp.entity.FtthCtoPort;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.financial.NetworkProject;
import br.dev.xb.isperp.entity.financial.UserCashCustody;
import br.dev.xb.isperp.ftth.FtthPortStatus;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.FtthCtoPortRepository;
import br.dev.xb.isperp.repository.FtthCtoRepository;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.financial.UserCashCustodyRepository;
import br.dev.xb.isperp.service.financial.NetworkProjectService;
import br.dev.xb.isperp.service.financial.SentinelWatchdogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class NetworkProjectsAndSentinelIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private NetworkProjectService networkProjectService;

    @Autowired
    private SentinelWatchdogService sentinelWatchdogService;

    @Autowired
    private FtthCtoRepository ctoRepository;

    @Autowired
    private FtthCtoPortRepository ctoPortRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCashCustodyRepository cashCustodyRepository;

    @Test
    @DisplayName("Valida no PostgreSQL 17 real: Payback por Projeto de Rede, Direcionador Comercial e Varredura Pericial do Sentinela IA")
    void shouldVerifyNetworkPaybackAndSentinelOnPostgres17() {
        // 1. Criar Projeto de Expansão de Rede (CAPEX R$ 60.000,00)
        NetworkProject project = networkProjectService.createProject(NetworkProjectRequest.builder()
                .name("Expansão Bairro Aeroporto")
                .neighborhood("Aeroporto")
                .city("Santarém")
                .budgetAmount(new BigDecimal("60000.00"))
                .targetSubscribers(100)
                .startDate(LocalDate.now())
                .notes("Imersão em fibra óptica para atendimento PME e residencial")
                .build());

        assertThat(project.getId()).isNotNull();

        // 2. Criar CTO e associar ao projeto
        FtthCto cto = ctoRepository.save(FtthCto.builder()
                .name("CTO-AERO-01")
                .latitude(new BigDecimal("-2.43890000"))
                .longitude(new BigDecimal("-54.71230000"))
                .totalPorts(16)
                .splitterType("BALANCED_1_16")
                .status("ATIVA")
                .projectId(project.getId())
                .build());

        // Criar portas na CTO (1 porta ocupada e 15 livres)
        Customer customer = customerRepository.save(Customer.builder()
                .name("Cliente Bairro Aeroporto")
                .cpf(generateValidCpf())
                .email("aero." + System.currentTimeMillis() + "@isp.com.br")
                .phone("93988887777")
                .build());

        ctoPortRepository.save(FtthCtoPort.builder()
                .ctoId(cto.getId())
                .portNumber(1)
                .status(FtthPortStatus.OCUPADA)
                .customerId(customer.getId())
                .build());

        // 3. Executar cálculo de Payback por Projeto
        List<NetworkProjectPaybackDto> paybacks = networkProjectService.getAllProjectsWithPayback();
        assertThat(paybacks).isNotEmpty();

        NetworkProjectPaybackDto projectPayback = paybacks.stream()
                .filter(p -> p.getProjectId().equals(project.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(projectPayback.getTotalPorts()).isEqualTo(16);
        assertThat(projectPayback.getOccupiedPorts()).isEqualTo(1);
        // Ocupação baixa (1/16 = 6.3%) deve ativar alerta de 'DINHEIRO DORMINDO NO POSTE'
        assertThat(projectPayback.getCommercialDirectionAlert()).contains("DINHEIRO DORMINDO NO POSTE");

        // 4. Testar Sentinela IA: criar retenção excessiva de dinheiro vivo com técnico
        User technician = userRepository.save(User.builder()
                .name("Carlos Técnico Campo")
                .email("carlos.campo." + System.currentTimeMillis() + "@isperp.dev")
                .password("hash123")
                .role(br.dev.xb.isperp.entity.UserRole.TECHNICIAN)
                .active(true)
                .cpf(generateValidCpf())
                .build());

        cashCustodyRepository.save(UserCashCustody.builder()
                .user(technician)
                .currentBalance(new BigDecimal("1800.00")) // Acima do limiar de R$ 1.000
                .build());

        // 5. Disparar varredura do Sentinela IA
        List<SentinelAuditLogDto> alerts = sentinelWatchdogService.triggerManualSweep();
        assertThat(alerts).isNotEmpty();

        SentinelAuditLogDto cashAlert = alerts.stream()
                .filter(a -> "CASH_CONCENTRATION".equals(a.getAuditType()))
                .findFirst()
                .orElseThrow();

        assertThat(cashAlert.getDescription()).contains("Carlos Técnico Campo");
        assertThat(cashAlert.getGeminiAnalysis()).isNotBlank();
        assertThat(cashAlert.getRecommendedAction()).isNotBlank();

        // 6. Resolver o alerta pericial
        SentinelAuditLogDto resolved = sentinelWatchdogService.resolveAuditAlert(cashAlert.getId());
        assertThat(resolved.getResolved()).isTrue();
    }

    private String generateValidCpf() {
        int[] digits = new int[11];
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 9; i++) {
            digits[i] = rnd.nextInt(10);
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += digits[i] * (10 - i);
        }
        int rem = sum % 11;
        digits[9] = rem < 2 ? 0 : 11 - rem;
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += digits[i] * (11 - i);
        }
        rem = sum % 11;
        digits[10] = rem < 2 ? 0 : 11 - rem;
        StringBuilder sb = new StringBuilder();
        for (int d : digits) {
            sb.append(d);
        }
        return sb.toString();
    }
}
