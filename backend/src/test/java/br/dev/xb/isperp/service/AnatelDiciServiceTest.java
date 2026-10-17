package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.AnatelDiciAccessGroupDto;
import br.dev.xb.isperp.api.dto.AnatelDiciReportResponse;
import br.dev.xb.isperp.client.GeoCepClient;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.FiscalCompany;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.FiscalCompanyRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class AnatelDiciServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FiscalCompanyRepository fiscalCompanyRepository;

    @Mock
    private GeoCepClient geoCepClient;

    @InjectMocks
    private AnatelDiciService anatelDiciService;

    private FiscalCompany activeCompany;
    private Plan plan400;
    private Plan plan800;
    private Customer customerPf;
    private Customer customerPj;
    private Customer customerGov;

    @BeforeEach
    void setUp() {
        activeCompany = FiscalCompany.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .cnpj("12.345.678/0001-99")
                .razaoSocial("XinguBit Fibra Telecom Ltda")
                .codigoIbge("1500602")
                .cidade("Altamira")
                .uf("PA")
                .isActive(true)
                .build();

        plan400 = Plan.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Fibra 400 Mega")
                .downloadSpeed(400)
                .uploadSpeed(200)
                .price(new BigDecimal("119.90"))
                .build();

        plan800 = Plan.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Fibra Empresarial 800 Mega")
                .downloadSpeed(800)
                .uploadSpeed(400)
                .price(new BigDecimal("249.90"))
                .build();

        customerPf = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("João Assinante PF")
                .cpf("12345678901")
                .city("Altamira")
                .state("PA")
                .zipCode("68371-000")
                .isGovernment(false)
                .build();

        customerPj = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Comércio Xingu Ltda")
                .cpf("98765432000188")
                .city("Vitória do Xingu")
                .state("PA")
                .zipCode("68383-000")
                .isGovernment(false)
                .build();

        customerGov = Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .name("Prefeitura Municipal de Altamira")
                .cpf("05123456000100")
                .city("Altamira")
                .state("PA")
                .zipCode("68371-010")
                .isGovernment(true)
                .build();
    }

    @Test
    @DisplayName("Deve consolidar contratos ACTIVE e SUSPENDED por IBGE, Tipo de Cliente (PF, PJ, GOVERNO) e Velocidade com cache IBGE")
    void shouldGenerateAnatelDiciReportWithActiveAndSuspendedAndGeoCepFallback() {
        Contract c1ActivePf = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerPf.getId())
                .planId(plan400.getId())
                .contractNumber("CTR-001")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("119.90"))
                .installationAddress("Av. Tancredo Neves, 100")
                .city("Altamira")
                .state("PA")
                .zipCode("68371-000")
                .build();

        Contract c2SuspendedPf = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerPf.getId())
                .planId(plan400.getId())
                .contractNumber("CTR-002")
                .status(Contract.ContractStatus.SUSPENDED)
                .monthlyFee(new BigDecimal("119.90"))
                .installationAddress("Rua 7 de Setembro, 200")
                .city("Altamira")
                .state("PA")
                .zipCode("68371-000")
                .ibgeCode("1500602")
                .build();

        Contract c3ActivePj = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerPj.getId())
                .planId(plan800.getId())
                .contractNumber("CTR-003")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("249.90"))
                .installationAddress("Av. Manoel Félix, 50")
                .city("Vitória do Xingu")
                .state("PA")
                .zipCode("68383-000")
                .build();

        Contract c4ActiveGov = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerGov.getId())
                .planId(plan800.getId())
                .contractNumber("CTR-004")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("249.90"))
                .installationAddress("Rua Coronel José Porfírio, 500")
                .city("Altamira")
                .state("PA")
                .zipCode("68371-010")
                .build();

        when(fiscalCompanyRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(activeCompany));
        when(contractRepository.findByStatusIn(any())).thenReturn(List.of(c1ActivePf, c2SuspendedPf, c3ActivePj, c4ActiveGov));
        when(customerRepository.findAllById(any())).thenReturn(List.of(customerPf, customerPj, customerGov));
        when(planRepository.findAllById(any())).thenReturn(List.of(plan400, plan800));

        // Simula resolução GeoCEP para Altamira e fallback local para Vitória do Xingu
        when(geoCepClient.resolveIbgeWithFallback(any(), eq("68371-000"), any(), any(), any(), any(), any()))
                .thenReturn(GeoCepClient.IbgeResolutionResult.builder()
                        .ibgeCode("1500602")
                        .municipalityName("Altamira")
                        .uf("PA")
                        .resolutionSource("GEOCEP_API")
                        .build());

        when(geoCepClient.resolveIbgeWithFallback(any(), eq("68383-000"), any(), any(), any(), any(), any()))
                .thenReturn(GeoCepClient.IbgeResolutionResult.builder()
                        .ibgeCode("1508357")
                        .municipalityName("Vitória do Xingu")
                        .uf("PA")
                        .resolutionSource("LOCAL_FALLBACK")
                        .build());

        when(geoCepClient.resolveIbgeWithFallback(any(), eq("68371-010"), any(), any(), any(), any(), any()))
                .thenReturn(GeoCepClient.IbgeResolutionResult.builder()
                        .ibgeCode("1500602")
                        .municipalityName("Altamira")
                        .uf("PA")
                        .resolutionSource("GEOCEP_API")
                        .build());

        AnatelDiciReportResponse report = anatelDiciService.generateReport("2026-10");

        assertThat(report.getYearMonth()).isEqualTo("2026-10");
        assertThat(report.getActiveAccessesCount()).isEqualTo(3);
        assertThat(report.getSuspendedAccessesCount()).isEqualTo(1);
        assertThat(report.getTotalAccessesInService()).isEqualTo(4);
        assertThat(report.getPfAccessesCount()).isEqualTo(2);
        assertThat(report.getPjAccessesCount()).isEqualTo(1);
        assertThat(report.getGovernmentAccessesCount()).isEqualTo(1);
        assertThat(report.getMunicipalitiesCount()).isEqualTo(2);
        assertThat(report.getRows()).hasSize(3);

        // Grupo 1: Altamira (1500602) - GOVERNO - 800 Mbps
        AnatelDiciAccessGroupDto govGroup = report.getRows().stream()
                .filter(g -> "GOVERNO".equals(g.getCustomerType()))
                .findFirst().orElseThrow();
        assertThat(govGroup.getIbgeCode()).isEqualTo("1500602");
        assertThat(govGroup.getTotalInServiceCount()).isEqualTo(1);

        // Grupo 2: Altamira (1500602) - PF - 400 Mbps (1 ativo + 1 suspenso = 2 total)
        AnatelDiciAccessGroupDto pfGroup = report.getRows().stream()
                .filter(g -> "PF".equals(g.getCustomerType()))
                .findFirst().orElseThrow();
        assertThat(pfGroup.getIbgeCode()).isEqualTo("1500602");
        assertThat(pfGroup.getActiveCount()).isEqualTo(1);
        assertThat(pfGroup.getSuspendedCount()).isEqualTo(1);
        assertThat(pfGroup.getTotalInServiceCount()).isEqualTo(2);

        // Grupo 3: Vitória do Xingu (1508357) - PJ - 800 Mbps (LOCAL_FALLBACK)
        AnatelDiciAccessGroupDto pjGroup = report.getRows().stream()
                .filter(g -> "PJ".equals(g.getCustomerType()))
                .findFirst().orElseThrow();
        assertThat(pjGroup.getIbgeCode()).isEqualTo("1508357");
        assertThat(pjGroup.getIbgeResolutionSource()).isEqualTo("LOCAL_FALLBACK");
        assertThat(pjGroup.getTotalInServiceCount()).isEqualTo(1);

        // Verifica que os 3 contratos sem ibgeCode foram atualizados em lote (cache em banco)
        verify(contractRepository, times(1)).saveAll(argThat(iterable -> {
            List<Contract> saved = (List<Contract>) iterable;
            return saved.size() == 3;
        }));
    }

    @Test
    @DisplayName("Deve exportar CSV oficial Anatel DICI separado por ponto-e-vírgula (UTF-8)")
    void shouldExportOfficialAnatelDiciCsv() {
        Contract c1 = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(customerPf.getId())
                .planId(plan400.getId())
                .contractNumber("CTR-001")
                .status(Contract.ContractStatus.ACTIVE)
                .monthlyFee(new BigDecimal("119.90"))
                .installationAddress("Av. Tancredo Neves, 100")
                .city("Altamira")
                .state("PA")
                .zipCode("68371-000")
                .ibgeCode("1500602")
                .build();

        when(fiscalCompanyRepository.findFirstByIsActiveTrue()).thenReturn(Optional.of(activeCompany));
        when(contractRepository.findByStatusIn(any())).thenReturn(List.of(c1));
        when(customerRepository.findAllById(any())).thenReturn(List.of(customerPf));
        when(planRepository.findAllById(any())).thenReturn(List.of(plan400));
        when(geoCepClient.resolveIbgeWithFallback(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(GeoCepClient.IbgeResolutionResult.builder()
                        .ibgeCode("1500602")
                        .municipalityName("Altamira")
                        .uf("PA")
                        .resolutionSource("GEOCEP_API")
                        .build());

        byte[] csvBytes = anatelDiciService.exportCsvBytes("2026-10");
        String csvText = new String(csvBytes, StandardCharsets.UTF_8);

        assertThat(csvText).startsWith("CNPJ;ANO;MES;CODIGO_IBGE;MUNICIPIO;UF;TIPO_CLIENTE;TECNOLOGIA;MEIO_TRANSMISSAO;VELOCIDADE_DOWN_MBPS;VELOCIDADE_UP_MBPS;ACESSOS_ATIVOS;ACESSOS_SUSPENSOS;ACESSOS_TOTAL\n");
        assertThat(csvText).contains("12345678000199;2026;10;1500602;Altamira;PA;PF;FTTH;FIBRA;400;200;1;0;1");
    }
}
