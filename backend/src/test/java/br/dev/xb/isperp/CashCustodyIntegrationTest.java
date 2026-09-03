package br.dev.xb.isperp;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.*;
import br.dev.xb.isperp.entity.financial.*;
import br.dev.xb.isperp.repository.*;
import br.dev.xb.isperp.repository.financial.*;
import br.dev.xb.isperp.service.financial.CashCustodyService;
import br.dev.xb.isperp.service.financial.MaterialCustodyService;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("null")
class CashCustodyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CashCustodyService cashCustodyService;

    @Autowired
    private MaterialCustodyService materialCustodyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserCashCustodyRepository cashCustodyRepository;

    @Autowired
    private CashTransferLogRepository cashTransferLogRepository;

    @Autowired
    private BankDepositConfirmationRepository bankDepositRepository;

    @Autowired
    private UserMaterialCustodyRepository materialCustodyRepository;

    @Test
    @DisplayName("Ciclo Real E2E com PostgreSQL 17: Recebimento em Campo ➔ Duplo Aceite de Gaveta ➔ Auditoria CFO ➔ Carga Material por CPF")
    void shouldExecuteCompleteRealLifeCustodyCycleOnPostgres17() {
        // 1. Criar Usuários com CPF no PostgreSQL 17 Real
        User techCarlos = userRepository.save(User.builder()
                .name("Carlos Técnico Real")
                .email("carlos.real." + System.currentTimeMillis() + "@isperp.com.br")
                .password("$2a$10$dummyHashPasswordForTests123")
                .cpf("111.222.333-44")
                .role(UserRole.TECHNICIAN)
                .active(true)
                .build());

        User cashierMaria = userRepository.save(User.builder()
                .name("Maria Caixa Real")
                .email("maria.real." + System.currentTimeMillis() + "@isperp.com.br")
                .password("$2a$10$dummyHashPasswordForTests123")
                .cpf("555.666.777-88")
                .role(UserRole.ATTENDANT)
                .active(true)
                .build());

        User cfoRoberto = userRepository.save(User.builder()
                .name("Roberto CFO Real")
                .email("roberto.cfo." + System.currentTimeMillis() + "@isperp.com.br")
                .password("$2a$10$dummyHashPasswordForTests123")
                .cpf("999.888.777-66")
                .role(UserRole.CFO)
                .active(true)
                .build());

        // 2. Criar Cliente, Plano, Contrato e Fatura em Aberto
        Customer customer = customerRepository.save(Customer.builder()
                .name("João Assinante Real")
                .cpf(generateValidCpf())
                .email("joao.real." + System.currentTimeMillis() + "@gmail.com")
                .phone("11988887777")
                .build());

        Plan plan = planRepository.save(Plan.builder()
                .name("Plano 500 Mega Real")
                .downloadSpeed(500)
                .uploadSpeed(250)
                .price(new BigDecimal("99.90"))
                .build());

        Contract contract = contractRepository.save(Contract.builder()
                .customerId(customer.getId())
                .planId(plan.getId())
                .contractNumber("CTR-CUSTODIA-" + System.currentTimeMillis())
                .monthlyFee(new BigDecimal("99.90"))
                .dueDay(10)
                .installationAddress("Rua das Flores, 123")
                .city("Altamira")
                .state("PA")
                .zipCode("68370-000")
                .status(Contract.ContractStatus.ACTIVE)
                .build());

        Invoice invoice = invoiceRepository.save(Invoice.builder()
                .contractId(contract.getId())
                .customerId(customer.getId())
                .amount(new BigDecimal("300.00")) // Taxa de ativação
                .dueDate(LocalDate.now().plusDays(2))
                .status(Invoice.InvoiceStatus.PENDING)
                .build());

        // =========================================================================
        // CENÁRIO 1: Carlos recebe R$ 300,00 em dinheiro vivo na zona rural
        // =========================================================================
        CashSettlementRequest settlementRequest = CashSettlementRequest.builder()
                .invoiceId(invoice.getId())
                .amount(new BigDecimal("300.00"))
                .receiptNumber("REC-RURAL-001")
                .notes("Recebido em espécie no ato da instalação")
                .build();

        CashCustodyDto carlosCustodyAfterSettlement = cashCustodyService.recordCashSettlement(techCarlos.getId(), settlementRequest);

        assertThat(carlosCustodyAfterSettlement.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("300.00"));

        // Valida no banco PostgreSQL 17 real
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getId()).orElseThrow();
        assertThat(updatedInvoice.getStatus()).isEqualTo(Invoice.InvoiceStatus.PAID);
        assertThat(updatedInvoice.getPaymentMethod()).isEqualTo("DINHEIRO_ESPECIE");
        assertThat(updatedInvoice.getSettledInCashByUserId()).isEqualTo(techCarlos.getId());
        assertThat(updatedInvoice.getReceiptNumber()).isEqualTo("REC-RURAL-001");

        UserCashCustody carlosDbCustody = cashCustodyRepository.findByUserId(techCarlos.getId()).orElseThrow();
        assertThat(carlosDbCustody.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("300.00"));

        // =========================================================================
        // CENÁRIO 2: Passagem de Gaveta (Carlos entrega o dinheiro para Maria no escritório)
        // =========================================================================
        CashTransferRequest transferRequest = CashTransferRequest.builder()
                .receiverUserId(cashierMaria.getId())
                .amount(new BigDecimal("300.00"))
                .reason("Fechamento de rota externa e entrega na tesouraria")
                .build();

        CashTransferResponseDto transferResponse = cashCustodyService.requestTransfer(techCarlos.getId(), transferRequest);
        assertThat(transferResponse.getStatus()).isEqualTo(CashTransferStatus.PENDING_ACCEPTANCE);

        // Antes de Maria aceitar, o saldo no banco PostgreSQL 17 AINDA PERTENCE a Carlos!
        carlosDbCustody = cashCustodyRepository.findByUserId(techCarlos.getId()).orElseThrow();
        assertThat(carlosDbCustody.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("300.00"));

        // Maria conta as cédulas no balcão e confirma o aceite
        CashTransferResponseDto acceptedTransfer = cashCustodyService.respondTransfer(cashierMaria.getId(), transferResponse.getId(), true);
        assertThat(acceptedTransfer.getStatus()).isEqualTo(CashTransferStatus.ACCEPTED);

        // Após o duplo aceite, valida os saldos no banco real
        carlosDbCustody = cashCustodyRepository.findByUserId(techCarlos.getId()).orElseThrow();
        assertThat(carlosDbCustody.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        UserCashCustody mariaDbCustody = cashCustodyRepository.findByUserId(cashierMaria.getId()).orElseThrow();
        assertThat(mariaDbCustody.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("300.00"));

        // =========================================================================
        // CENÁRIO 3: Maria vai ao banco, deposita e anexa o comprovante
        // =========================================================================
        BankDepositRequest depositRequest = BankDepositRequest.builder()
                .amount(new BigDecimal("300.00"))
                .bankName("Banco do Brasil")
                .bankAgency("1234-5")
                .bankAccount("998877-1")
                .receiptFileUrl("https://storage.isperp.dev/receipts/deposito_300_bb.pdf")
                .notes("Depósito de arrecadação do dia")
                .build();

        BankDepositResponseDto depositResponse = cashCustodyService.submitBankDeposit(cashierMaria.getId(), depositRequest);
        assertThat(depositResponse.getStatus()).isEqualTo(BankDepositStatus.PENDING_AUDIT);

        // O saldo devedor de Maria CONTINUA 300.00 até o CFO auditar o extrato!
        mariaDbCustody = cashCustodyRepository.findByUserId(cashierMaria.getId()).orElseThrow();
        assertThat(mariaDbCustody.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("300.00"));

        // =========================================================================
        // CENÁRIO 4: Roberto (CFO) audita o extrato bancário real e aprova
        // =========================================================================
        BankDepositAuditRequest auditRequest = BankDepositAuditRequest.builder()
                .approved(true)
                .notes("Conferido no extrato do Banco do Brasil. Crédito compensado com sucesso.")
                .build();

        BankDepositResponseDto auditedDeposit = cashCustodyService.auditBankDeposit(cfoRoberto.getId(), depositResponse.getId(), auditRequest);
        assertThat(auditedDeposit.getStatus()).isEqualTo(BankDepositStatus.CONFIRMED_IN_BANK);
        assertThat(auditedDeposit.getAuditedByUserId()).isEqualTo(cfoRoberto.getId());

        // Agora sim: a responsabilidade de Maria é baixada para ZERO no banco de dados!
        mariaDbCustody = cashCustodyRepository.findByUserId(cashierMaria.getId()).orElseThrow();
        assertThat(mariaDbCustody.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        // =========================================================================
        // CENÁRIO 5: Custódia Material por CPF (Carga de ONT e Ferramenta no Técnico)
        // =========================================================================
        String serialNumber = "HWTC-" + System.currentTimeMillis();
        MaterialCustodyDto materialDto = MaterialCustodyDto.builder()
                .itemName("ONT Huawei Wi-Fi 6 GPON Real")
                .itemType(MaterialType.ONT)
                .serialNumber(serialNumber)
                .macAddress("AA:BB:CC:DD:EE:FF")
                .quantity(BigDecimal.ONE)
                .unit("UN")
                .notes("Retirado do Almoxarifado Central")
                .build();

        MaterialCustodyDto allocatedMaterial = materialCustodyService.allocateMaterialToUser(techCarlos.getId(), materialDto);
        assertThat(allocatedMaterial.getSerialNumber()).isEqualTo(serialNumber);

        // Consulta no Postgres 17 real
        Optional<UserMaterialCustody> materialInDb = materialCustodyRepository.findBySerialNumber(serialNumber);
        assertTrue(materialInDb.isPresent());
        assertThat(materialInDb.get().getUser().getId()).isEqualTo(techCarlos.getId());

        // Baixa automática do equipamento na conclusão da O.S.
        materialCustodyService.consumeMaterialOnWorkOrder(techCarlos.getId(), serialNumber, BigDecimal.ONE);
        Optional<UserMaterialCustody> materialAfterOs = materialCustodyRepository.findBySerialNumber(serialNumber);
        assertThat(materialAfterOs).isEmpty();
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
