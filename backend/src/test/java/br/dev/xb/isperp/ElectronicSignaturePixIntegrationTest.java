package br.dev.xb.isperp;

import br.dev.xb.isperp.dto.*;
import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.service.ElectronicSignatureService;
import br.dev.xb.isperp.signature.FallbackMethod;
import br.dev.xb.isperp.signature.SignatureStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("null")
class ElectronicSignaturePixIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ElectronicSignatureService signatureService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Test
    @DisplayName("Valida no PostgreSQL 17 real: Bloqueio por Divergência de CPF, Fallback e Assinatura com Desconto de R$ 1,00")
    void shouldVerifyPixSignatureStrictCpfAndInvoiceDiscountOnPostgres17() {
        // 1. Criar Cliente com CPF Válido
        String titularCpf = generateValidCpf();
        Customer customer = customerRepository.save(Customer.builder()
                .name("Mariana Titular Contrato")
                .cpf(titularCpf)
                .email("mariana." + System.currentTimeMillis() + "@isp.com.br")
                .phone("11988887777")
                .build());

        // 2. Criar Plano e Contrato
        Plan plan = planRepository.save(Plan.builder()
                .name("Fibra 500 Mega Residencial")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(new BigDecimal("99.90"))
                .build());

        Contract contract = contractRepository.save(Contract.builder()
                .customerId(customer.getId())
                .planId(plan.getId())
                .contractNumber("CTR-PIX-" + System.currentTimeMillis())
                .monthlyFee(new BigDecimal("99.90"))
                .dueDay(15)
                .installationAddress("Rua das Palmeiras, 500")
                .city("Santarém")
                .state("PA")
                .zipCode("68000-000")
                .status(Contract.ContractStatus.DRAFT)
                .build());

        // 3. Criar Fatura Pendente para o Contrato
        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .contractId(contract.getId())
                .customerId(customer.getId())
                .amount(new BigDecimal("99.90"))
                .discountAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.now().plusDays(15))
                .status(Invoice.InvoiceStatus.PENDING)
                .build());

        // 4. Iniciar Sessão de Assinatura Eletrônica via Pix (R$ 1,00)
        CreateSignatureSessionRequest sessionReq = CreateSignatureSessionRequest.builder()
                .contractId(contract.getId())
                .symbolicAmount(new BigDecimal("1.00"))
                .build();

        SignatureSessionResponse session = signatureService.createSignatureSession(sessionReq, "http://localhost:5173");
        assertThat(session.getToken()).isNotBlank();
        assertThat(session.getStatus()).isEqualTo(SignatureStatus.PENDING);
        assertThat(session.getPixTxid()).isNotBlank();

        // -------------------------------------------------------------
        // CENÁRIO 1: TENTATIVA COM CPF DIVERGENTE (Ex: Marido ou Terceiro)
        // -------------------------------------------------------------
        String divergenteCpf = generateValidCpf();
        while (divergenteCpf.equals(titularCpf)) {
            divergenteCpf = generateValidCpf();
        }

        PixSignatureWebhookRequest fakePaymentDivergent = PixSignatureWebhookRequest.builder()
                .txid(session.getPixTxid())
                .endToEndId("E00000000202609020000s1234567890")
                .payerName("Roberto Marido Terceiro")
                .payerCpfCnpj(divergenteCpf)
                .bankName("Banco Terceiro S.A.")
                .ispb("00000000")
                .amount(new BigDecimal("1.00"))
                .build();

        SignatureSessionResponse divergentResp = signatureService.processPixSignatureWebhook(fakePaymentDivergent);
        assertThat(divergentResp.getStatus()).isEqualTo(SignatureStatus.REJECTED_DIVERGENT_DOCUMENT);
        assertThat(divergentResp.getRejectionReason()).contains("divergência de titularidade");

        // Fatura NÃO deve receber desconto e contrato NÃO deve ser assinado
        Invoice invoiceAfterRejection = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(invoiceAfterRejection.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        Contract contractAfterRejection = contractRepository.findById(contract.getId()).orElseThrow();
        assertThat(contractAfterRejection.getStatus()).isEqualTo(Contract.ContractStatus.DRAFT);

        // Testar Seleção de Fallback Oficial (Gov.br)
        SignatureSessionResponse fallbackResp = signatureService.selectFallbackMethod(
                session.getToken(),
                FallbackMethod.GOV_BR,
                "Cliente não possui Pix em conta própria, optou por assinar com selo Ouro no Gov.br"
        );
        assertThat(fallbackResp.getFallbackMethod()).isEqualTo(FallbackMethod.GOV_BR);

        // -------------------------------------------------------------
        // CENÁRIO 2: PAGAMENTO LEGÍTIMO COM O MESMO CPF DO TITULAR (BACEN)
        // -------------------------------------------------------------
        PixSignatureWebhookRequest legitimatePayment = PixSignatureWebhookRequest.builder()
                .txid(session.getPixTxid())
                .endToEndId("E00000000202609022359s9876543210")
                .payerName("Mariana Titular Contrato")
                .payerCpfCnpj(titularCpf)
                .bankName("Nubank S.A.")
                .ispb("18236120")
                .amount(new BigDecimal("1.00"))
                .build();

        SignatureSessionResponse successResp = signatureService.processPixSignatureWebhook(legitimatePayment);
        assertThat(successResp.getStatus()).isEqualTo(SignatureStatus.SIGNED);
        assertThat(successResp.getPixEndToEndId()).isEqualTo("E00000000202609022359s9876543210");
        assertThat(successResp.getSignedPdfUrl()).isNotBlank();
        assertThat(successResp.getForensicCertificatePdfUrl()).isNotBlank();
        assertThat(successResp.getOnboardingCreditAmount()).isEqualByComparingTo(new BigDecimal("1.00"));

        // Verificar se a Fatura Pendente recebeu o Desconto de R$ 1,00
        Invoice invoiceAfterSuccess = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(invoiceAfterSuccess.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(successResp.getDiscountAppliedInvoiceId()).isEqualTo(invoice.getId());

        // Verificar se o Contrato avançou para PENDING_INSTALLATION
        Contract contractAfterSuccess = contractRepository.findById(contract.getId()).orElseThrow();
        assertThat(contractAfterSuccess.getStatus()).isEqualTo(Contract.ContractStatus.PENDING_INSTALLATION);

        // Verificar se o Certificado Forense de Autenticidade foi gerado com as evidências do BACEN
        SignaturePublicViewResponse publicView = signatureService.getPublicSignatureView(session.getToken(), null, null, null, null);
        assertThat(publicView.getRenderedContent()).contains("CERTIFICADO DE AUTENTICIDADE E ASSINATURA ELETRÔNICA AVANÇADA");
        assertThat(publicView.getRenderedContent()).contains("Medida Provisória nº 2.200-2/2001");
        assertThat(publicView.getRenderedContent()).contains("E00000000202609022359s9876543210");
        assertThat(publicView.getRenderedContent()).contains("Nubank S.A.");
        assertThat(publicView.getDocumentSha256Hash()).isNotBlank();
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
