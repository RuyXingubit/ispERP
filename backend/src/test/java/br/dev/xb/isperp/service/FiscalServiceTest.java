package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.fiscal.FiscalGateway;
import br.dev.xb.isperp.fiscal.FiscalGatewayResolver;
import br.dev.xb.isperp.fiscal.FiscalGatewayType;
import br.dev.xb.isperp.fiscal.dto.CertificateUploadResult;
import br.dev.xb.isperp.fiscal.dto.NfcomCancelResult;
import br.dev.xb.isperp.fiscal.dto.NfcomIssueResult;
import br.dev.xb.isperp.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FiscalServiceTest {

    private FiscalCompanyRepository companyRepository;
    private FiscalGatewayConfigRepository configRepository;
    private NfcomRecordRepository nfcomRecordRepository;
    private InvoiceRepository invoiceRepository;
    private CustomerRepository customerRepository;
    private ContractRepository contractRepository;
    private PlanRepository planRepository;
    private FiscalGatewayResolver gatewayResolver;
    private FiscalGateway mockGateway;

    private FiscalService fiscalService;

    @BeforeEach
    void setUp() {
        companyRepository = Mockito.mock(FiscalCompanyRepository.class);
        configRepository = Mockito.mock(FiscalGatewayConfigRepository.class);
        nfcomRecordRepository = Mockito.mock(NfcomRecordRepository.class);
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        contractRepository = Mockito.mock(ContractRepository.class);
        planRepository = Mockito.mock(PlanRepository.class);
        gatewayResolver = Mockito.mock(FiscalGatewayResolver.class);
        mockGateway = Mockito.mock(FiscalGateway.class);

        fiscalService = new FiscalService(
                companyRepository,
                configRepository,
                nfcomRecordRepository,
                invoiceRepository,
                customerRepository,
                contractRepository,
                planRepository,
                gatewayResolver
        );
    }

    @Test
    @DisplayName("Deve salvar empresa e fazer onboarding no gateway fiscal")
    void testSaveCompany() {
        FiscalCompany company = FiscalCompany.builder()
                .id(UUID.randomUUID())
                .cnpj("12.345.678/0001-95")
                .razaoSocial("Provedor Xingu Telecom")
                .inscricaoEstadual("15999888")
                .logradouro("Rua Central")
                .numero("10")
                .bairro("Centro")
                .cidade("Altamira")
                .uf("PA")
                .cep("68370-000")
                .codigoIbge("1500602")
                .build();

        FiscalGatewayConfig config = FiscalGatewayConfig.builder()
                .companyId(company.getId())
                .gatewayType(FiscalGatewayType.XINGUBIT_PAY)
                .build();

        when(companyRepository.save(any(FiscalCompany.class))).thenReturn(company);
        when(gatewayResolver.resolve(any())).thenReturn(
                new FiscalGatewayResolver.ResolvedFiscalGateway(mockGateway, config, company)
        );
        when(mockGateway.registerCompany(any(), any())).thenReturn(true);
        when(mockGateway.configureNfcom(any(), any())).thenReturn(true);

        FiscalCompany saved = fiscalService.saveCompany(company, config);
        assertNotNull(saved);
        assertEquals("12.345.678/0001-95", saved.getCnpj());
    }

    @Test
    @DisplayName("Deve fazer upload do certificado A1 e atualizar a empresa")
    void testUploadCertificate() {
        UUID companyId = UUID.randomUUID();
        FiscalCompany company = FiscalCompany.builder()
                .id(companyId)
                .cnpj("12.345.678/0001-95")
                .hasCertificate(false)
                .build();

        FiscalGatewayConfig config = FiscalGatewayConfig.builder().companyId(companyId).build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(gatewayResolver.resolve(companyId)).thenReturn(
                new FiscalGatewayResolver.ResolvedFiscalGateway(mockGateway, config, company)
        );
        when(mockGateway.uploadCertificate(any(), any(), any())).thenReturn(
                CertificateUploadResult.builder()
                        .success(true)
                        .validUntil(LocalDateTime.now().plusYears(1))
                        .message("Certificado ativo")
                        .build()
        );

        CertificateUploadResult result = fiscalService.uploadCertificate(companyId, "FAKE_BYTES".getBytes(), "123456");
        assertTrue(result.isSuccess());
        assertTrue(company.getHasCertificate());
    }

    @Test
    @DisplayName("Deve emitir NFCom Modelo 62 com sucesso e atualizar fatura")
    void testIssueNfcomForInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();

        Customer customer = Customer.builder()
                .id(customerId)
                .name("Maria Silva")
                .cpf("529.982.247-25")
                .build();

        Plan plan = Plan.builder()
                .id(planId)
                .name("Fibra 500M")
                .build();

        Contract contract = Contract.builder()
                .id(contractId)
                .planId(planId)
                .customerId(customerId)
                .build();

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .customerId(customerId)
                .contractId(contractId)
                .amount(new BigDecimal("99.90"))
                .dueDate(LocalDate.now().plusDays(5))
                .status(Invoice.InvoiceStatus.PAID)
                .build();

        FiscalCompany company = FiscalCompany.builder()
                .id(UUID.randomUUID())
                .cnpj("12.345.678/0001-95")
                .razaoSocial("Provedor Xingu")
                .nfcomSerie("1")
                .nfcomProximoNumero(10)
                .build();

        FiscalGatewayConfig config = FiscalGatewayConfig.builder().companyId(company.getId()).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(gatewayResolver.resolve(any())).thenReturn(
                new FiscalGatewayResolver.ResolvedFiscalGateway(mockGateway, config, company)
        );

        String accessKey = "15260812345678000195620010000000101112345678";
        when(mockGateway.issueNfcom(any(), any(), any())).thenReturn(
                NfcomIssueResult.builder()
                        .success(true)
                        .chaveAcesso(accessKey)
                        .numero(10)
                        .serie("1")
                        .status("AUTORIZADA")
                        .protocoloAutorizacao("115260001234567")
                        .danfePdfUrl("https://pay.xingubit.com.br/v1/invoices/nfcom/" + accessKey + "/pdf")
                        .xmlUrl("https://pay.xingubit.com.br/v1/invoices/nfcom/" + accessKey + "/xml")
                        .build()
        );
        when(nfcomRecordRepository.save(any(NfcomRecord.class))).thenAnswer(i -> i.getArgument(0));

        NfcomRecord record = fiscalService.issueNfcomForInvoice(invoiceId);

        assertNotNull(record);
        assertEquals(accessKey, record.getChaveAcesso());
        assertEquals(Invoice.NfcomStatus.ISSUED, invoice.getNfcomStatus());
        assertEquals(accessKey, invoice.getNfcomKey());
    }

    @Test
    @DisplayName("Deve cancelar NFCom com sucesso")
    void testCancelNfcom() {
        UUID recordId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        NfcomRecord record = NfcomRecord.builder()
                .id(recordId)
                .companyId(UUID.randomUUID())
                .invoiceId(invoiceId)
                .chaveAcesso("15260812345678000195620010000000101112345678")
                .status("AUTORIZADA")
                .build();

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .nfcomStatus(Invoice.NfcomStatus.ISSUED)
                .build();

        FiscalCompany company = FiscalCompany.builder().id(record.getCompanyId()).build();
        FiscalGatewayConfig config = FiscalGatewayConfig.builder().companyId(company.getId()).build();

        when(nfcomRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(gatewayResolver.resolve(record.getCompanyId())).thenReturn(
                new FiscalGatewayResolver.ResolvedFiscalGateway(mockGateway, config, company)
        );
        when(mockGateway.cancelNfcom(any(), any(), any())).thenReturn(
                NfcomCancelResult.builder()
                        .success(true)
                        .chaveAcesso(record.getChaveAcesso())
                        .protocoloCancelamento("115269991234567")
                        .build()
        );

        NfcomCancelResult result = fiscalService.cancelNfcom(recordId, "Cancelamento solicitado");

        assertTrue(result.isSuccess());
        assertEquals("CANCELADA", record.getStatus());
        assertEquals(Invoice.NfcomStatus.CANCELLED, invoice.getNfcomStatus());
    }
}
