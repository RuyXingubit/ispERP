package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.mapper.ContractSignatureMapper;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.signature.DocumentType;
import br.dev.xb.isperp.signature.SignatureStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElectronicSignatureServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractTemplateRepository templateRepository;

    @Mock
    private ContractSignatureRepository signatureRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ContractTemplateService templateService;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @Mock
    private InvoiceRepository invoiceRepository;

    private final ContractTemplateEngine templateEngine = new ContractTemplateEngine();
    private final ContractSignatureMapper signatureMapper = Mappers.getMapper(ContractSignatureMapper.class);

    private ElectronicSignatureService signatureService;

    @BeforeEach
    void setUp() {
        signatureService = new ElectronicSignatureService(
                contractRepository,
                templateRepository,
                signatureRepository,
                customerRepository,
                companyRepository,
                planRepository,
                templateEngine,
                templateService,
                signatureMapper,
                domainEventPublisher,
                invoiceRepository
        );
    }

    @Test
    @DisplayName("Deve criar sessão de assinatura com token seguro e Pix dinâmico")
    void testCreateSignatureSession() {
        UUID contractId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .planId(planId)
                .contractNumber("CTR-2026-0100")
                .monthlyFee(BigDecimal.valueOf(89.90))
                .dueDay(10)
                .installationAddress("Rua Principal, 100")
                .createdAt(LocalDateTime.now())
                .build();

        Customer customer = Customer.builder()
                .id(customerId)
                .name("Carlos Alberto")
                .cpf("98765432100")
                .build();

        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 400 Mega")
                .downloadSpeed(400)
                .uploadSpeed(200)
                .price(BigDecimal.valueOf(89.90))
                .build();

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(companyRepository.findAll()).thenReturn(List.of());
        when(templateRepository.findFirstByDocumentTypeAndIsActiveTrueOrderByVersionDesc(DocumentType.SERVICE_AGREEMENT))
                .thenReturn(Optional.empty());
        when(signatureRepository.save(any(ContractSignature.class))).thenAnswer(i -> {
            ContractSignature cs = i.getArgument(0);
            cs.setId(UUID.randomUUID());
            return cs;
        });

        CreateSignatureSessionRequest req = CreateSignatureSessionRequest.builder()
                .contractId(contractId)
                .symbolicAmount(BigDecimal.valueOf(1.00))
                .build();

        SignatureSessionResponse session = signatureService.createSignatureSession(req, "https://erp.provedor.com.br");

        assertThat(session).isNotNull();
        assertThat(session.getToken()).isNotBlank();
        assertThat(session.getSignatureUrl()).startsWith("https://erp.provedor.com.br/sign/");
        assertThat(session.getStatus()).isEqualTo(SignatureStatus.PENDING);
        assertThat(session.getSymbolicAmount()).isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName("Anti-Fraude: Deve APROVAR assinatura quando o CPF do pagador retornado pelo BACEN for IDÊNTICO ao titular")
    void testProcessPixSignatureWebhookSuccessWhenSameCpf() {
        UUID contractId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String txid = "SIG123456789";

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .contractNumber("CTR-2026-0001")
                .status(Contract.ContractStatus.PENDING_INSTALLATION)
                .build();

        Customer customer = Customer.builder()
                .id(customerId)
                .name("Ana Paula Silva")
                .cpf("11122233344")
                .build();

        ContractSignature signature = ContractSignature.builder()
                .id(UUID.randomUUID())
                .contractId(contractId)
                .token("tok123")
                .pixTxid(txid)
                .status(SignatureStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .renderedContentSnapshot("Texto do contrato...")
                .build();

        when(signatureRepository.findByPixTxid(txid)).thenReturn(Optional.of(signature));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(companyRepository.findAll()).thenReturn(List.of());
        when(signatureRepository.save(any(ContractSignature.class))).thenAnswer(i -> i.getArgument(0));

        PixSignatureWebhookRequest webhook = PixSignatureWebhookRequest.builder()
                .txid(txid)
                .endToEndId("E12345678202608311200")
                .amount(BigDecimal.valueOf(1.00))
                .payerName("Ana Paula Silva")
                .payerCpfCnpj("111.222.333-44") // Mesmo CPF
                .bankName("Banco Inter S.A.")
                .ispb("00416968")
                .build();

        SignatureSessionResponse result = signatureService.processPixSignatureWebhook(webhook);

        assertThat(result.getStatus()).isEqualTo(SignatureStatus.SIGNED);
        assertThat(result.getPixEndToEndId()).isEqualTo("E12345678202608311200");
        assertThat(result.getPayerBankName()).isEqualTo("Banco Inter S.A.");
        assertThat(result.getSignedPdfUrl()).isNotBlank();
        verify(domainEventPublisher, times(1)).publish(any());
    }

    @Test
    @DisplayName("Anti-Fraude: Deve REJEITAR assinatura quando o CPF do pagador DIVERGIR do titular do contrato")
    void testProcessPixSignatureWebhookRejectWhenDifferentCpf() {
        UUID contractId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        String txid = "SIG987654321";

        Contract contract = Contract.builder()
                .id(contractId)
                .customerId(customerId)
                .contractNumber("CTR-2026-0002")
                .build();

        Customer customer = Customer.builder()
                .id(customerId)
                .name("Fernanda Costa")
                .cpf("11122233344") // Titular
                .build();

        ContractSignature signature = ContractSignature.builder()
                .id(UUID.randomUUID())
                .contractId(contractId)
                .token("tok999")
                .pixTxid(txid)
                .status(SignatureStatus.PENDING)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();

        when(signatureRepository.findByPixTxid(txid)).thenReturn(Optional.of(signature));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(signatureRepository.save(any(ContractSignature.class))).thenAnswer(i -> i.getArgument(0));

        PixSignatureWebhookRequest webhook = PixSignatureWebhookRequest.builder()
                .txid(txid)
                .endToEndId("E99999999202608311200")
                .amount(BigDecimal.valueOf(1.00))
                .payerName("Rodrigo Silva (Marido)")
                .payerCpfCnpj("999.888.777-66") // CPF de terceiro!
                .bankName("Banco Bradesco")
                .build();

        SignatureSessionResponse result = signatureService.processPixSignatureWebhook(webhook);

        assertThat(result.getStatus()).isEqualTo(SignatureStatus.REJECTED_DIVERGENT_DOCUMENT);
        assertThat(result.getRejectionReason()).contains("divergência de titularidade");
        verify(domainEventPublisher, never()).publish(any());
    }
}
