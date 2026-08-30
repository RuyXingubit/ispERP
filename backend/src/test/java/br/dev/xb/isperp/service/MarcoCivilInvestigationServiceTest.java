package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.MarcoCivilMapper;
import br.dev.xb.isperp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarcoCivilInvestigationServiceTest {

    @Mock
    private RadAcctRepository radAcctRepository;

    @Mock
    private OnuProvisioningRepository onuProvisioningRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private CgnatParserService cgnatParserService;

    @Mock
    private MarcoCivilReportRepository marcoCivilReportRepository;

    private MarcoCivilMapper marcoCivilMapper = Mappers.getMapper(MarcoCivilMapper.class);

    private MarcoCivilInvestigationService investigationService;

    @BeforeEach
    void setUp() {
        investigationService = new MarcoCivilInvestigationService(
                radAcctRepository,
                onuProvisioningRepository,
                contractRepository,
                customerRepository,
                planRepository,
                cgnatParserService,
                marcoCivilReportRepository,
                marcoCivilMapper
        );
        ReflectionTestUtils.setField(investigationService, "publicBaseUrl", "http://localhost:5173");
    }

    @Test
    @DisplayName("Deve identificar assinante via CGNAT reverso e sessão RADIUS")
    void testSearchSubscriberWithCgnat() {
        OffsetDateTime eventTime = OffsetDateTime.now().minusHours(2);
        UUID customerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();

        when(cgnatParserService.findPrivateIpForPublicPort("200.150.10.2", 1500))
                .thenReturn(Optional.of("100.64.1.50"));

        RadAcct session = RadAcct.builder()
                .radacctId(101L)
                .username("joaosilva")
                .framedIpAddress("100.64.1.50")
                .callingStationId("48:8F:5A:12:34:56")
                .acctStartTime(eventTime.minusHours(1))
                .acctStopTime(null)
                .build();

        when(radAcctRepository.findSessionByIpAndTimestamp(eq("100.64.1.50"), anyString(), eq(eventTime)))
                .thenReturn(List.of(session));

        OnuProvisioning onu = OnuProvisioning.builder()
                .id(UUID.randomUUID())
                .pppoeUser("joaosilva")
                .contractId(contractId)
                .customerId(customerId)
                .build();

        when(onuProvisioningRepository.findByPppoeUser("joaosilva")).thenReturn(Optional.of(onu));

        Contract contract = Contract.builder()
                .id(contractId)
                .contractNumber("CTR-2026-001")
                .customerId(customerId)
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        Customer customer = Customer.builder()
                .id(customerId)
                .name("João Silva")
                .cpf("123.456.789-00")
                .phone("(11) 98765-4321")
                .email("joao@gmail.com")
                .address("Rua das Flores, 100")
                .city("São Paulo")
                .state("SP")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        MarcoCivilSearchResult result = investigationService.searchSubscriber(MarcoCivilSearchRequest.builder()
                .ip("200.150.10.2")
                .port(1500)
                .timestamp(eventTime)
                .build());

        assertThat(result.isMatched()).isTrue();
        assertThat(result.isUsedCgnat()).isTrue();
        assertThat(result.getResolvedPrivateIp()).isEqualTo("100.64.1.50");
        assertThat(result.getUsername()).isEqualTo("joaosilva");
        assertThat(result.getCustomerName()).isEqualTo("João Silva");
        assertThat(result.getCustomerCpfCnpj()).isEqualTo("123.456.789-00");
    }

    @Test
    @DisplayName("Deve emitir laudo pericial com hash SHA-256 e URL de QR Code")
    void testGenerateOfficialReport() {
        OffsetDateTime eventTime = OffsetDateTime.now().minusDays(1);

        when(radAcctRepository.findSessionByIpAndTimestamp(anyString(), anyString(), any()))
                .thenReturn(List.of());

        when(marcoCivilReportRepository.save(any(MarcoCivilReport.class))).thenAnswer(i -> i.getArgument(0));

        MarcoCivilReportResponse response = investigationService.generateOfficialReport(MarcoCivilReportRequest.builder()
                .courtOrderNumber("OF-2026/894")
                .requesterAuthority("1ª Delegacia de Crimes Cibernéticos")
                .queriedIp("200.150.10.2")
                .queriedPort(1500)
                .queriedTimestamp(eventTime)
                .build());

        assertThat(response).isNotNull();
        assertThat(response.getValidationToken()).isNotBlank();
        assertThat(response.getSha256Hash()).hasSize(64); // Tamanho padrão de hash SHA-256 em hex
        assertThat(response.getPublicValidationUrl()).contains("/public/validar-laudo/");
    }

    @Test
    @DisplayName("Deve validar autenticidade pública de token emitido")
    void testValidatePublicToken() {
        MarcoCivilReport report = MarcoCivilReport.builder()
                .validationToken("token123")
                .sha256Hash("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .courtOrderNumber("OF-1234")
                .requesterAuthority("Polícia Civil SP")
                .queriedIp("200.150.10.2")
                .queriedPort(8080)
                .queriedTimestamp(OffsetDateTime.now())
                .matchedCustomerName("Carlos Alberto de Nobrega")
                .matchedCpfCnpj("11122233344")
                .createdAt(OffsetDateTime.now())
                .build();

        when(marcoCivilReportRepository.findByValidationToken("token123")).thenReturn(Optional.of(report));

        PublicValidationResponse validation = investigationService.validatePublicToken("token123");

        assertThat(validation.isValid()).isTrue();
        assertThat(validation.getCustomerNameMasked()).contains("***");
        assertThat(validation.getStatusMessage()).contains("DOCUMENTO AUTÊNTICO");
    }
}
