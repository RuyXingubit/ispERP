package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.entity.financial.*;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.mapper.CustodyMapper;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.financial.BankDepositConfirmationRepository;
import br.dev.xb.isperp.repository.financial.CashTransferLogRepository;
import br.dev.xb.isperp.repository.financial.UserCashCustodyRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashCustodyServiceTest {

    @Mock
    private UserCashCustodyRepository cashCustodyRepository;

    @Mock
    private CashTransferLogRepository cashTransferLogRepository;

    @Mock
    private BankDepositConfirmationRepository bankDepositRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    private final CustodyMapper custodyMapper = Mappers.getMapper(CustodyMapper.class);

    private CashCustodyService cashCustodyService;

    private User technician;
    private User cashier;
    private User cfo;
    private UUID technicianId;
    private UUID cashierId;
    private UUID cfoId;

    @BeforeEach
    void setUp() {
        cashCustodyService = new CashCustodyService(
                cashCustodyRepository,
                cashTransferLogRepository,
                bankDepositRepository,
                userRepository,
                invoiceRepository,
                domainEventPublisher,
                custodyMapper
        );

        technicianId = UUID.randomUUID();
        technician = User.builder()
                .id(technicianId)
                .name("Carlos Técnico")
                .email("carlos@isperp.com")
                .cpf("111.222.333-44")
                .role(UserRole.TECHNICIAN)
                .build();

        cashierId = UUID.randomUUID();
        cashier = User.builder()
                .id(cashierId)
                .name("Maria Caixa")
                .email("maria@isperp.com")
                .cpf("555.666.777-88")
                .role(UserRole.ATTENDANT)
                .build();

        cfoId = UUID.randomUUID();
        cfo = User.builder()
                .id(cfoId)
                .name("Roberto CFO")
                .email("roberto@isperp.com")
                .cpf("999.888.777-66")
                .role(UserRole.CFO)
                .build();
    }

    @Test
    @DisplayName("Deve registrar recebimento em espécie, quitar fatura e creditar na custódia pessoal do CPF do técnico")
    void shouldRecordCashSettlementSuccessfully() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .contractId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new BigDecimal("300.00"))
                .dueDate(LocalDate.now().plusDays(5))
                .status(Invoice.InvoiceStatus.PENDING)
                .build();

        UserCashCustody technicianCustody = UserCashCustody.builder()
                .id(UUID.randomUUID())
                .user(technician)
                .cpf(technician.getCpf())
                .currentBalance(BigDecimal.ZERO)
                .build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(cashCustodyRepository.findByUserId(technicianId)).thenReturn(Optional.of(technicianCustody));
        when(cashCustodyRepository.save(any(UserCashCustody.class))).thenAnswer(i -> i.getArgument(0));

        CashSettlementRequest request = CashSettlementRequest.builder()
                .invoiceId(invoiceId)
                .amount(new BigDecimal("300.00"))
                .receiptNumber("REC-12345")
                .build();

        CashCustodyDto result = cashCustodyService.recordCashSettlement(technicianId, request);

        assertThat(result.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(invoice.getStatus()).isEqualTo(Invoice.InvoiceStatus.PAID);
        assertThat(invoice.getPaymentMethod()).isEqualTo("DINHEIRO_ESPECIE");
        assertThat(invoice.getSettledInCashByUserId()).isEqualTo(technicianId);
        assertThat(invoice.getReceiptNumber()).isEqualTo("REC-12345");

        verify(domainEventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve impedir transferência de custódia se o saldo em mãos for menor que o valor solicitado")
    void shouldRejectTransferWhenInsufficientBalance() {
        UserCashCustody senderCustody = UserCashCustody.builder()
                .id(UUID.randomUUID())
                .user(technician)
                .currentBalance(new BigDecimal("100.00"))
                .build();

        when(cashCustodyRepository.findByUserId(technicianId)).thenReturn(Optional.of(senderCustody));

        CashTransferRequest request = CashTransferRequest.builder()
                .receiverUserId(cashierId)
                .amount(new BigDecimal("300.00"))
                .reason("Passagem de turno")
                .build();

        assertThatThrownBy(() -> cashCustodyService.requestTransfer(technicianId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Saldo de custódia insuficiente");
    }

    @Test
    @DisplayName("Deve concluir transferência com duplo aceite migrando o saldo de Carlos para Maria")
    void shouldCompleteTransferWithDualAcceptance() {
        UUID transferLogId = UUID.randomUUID();

        UserCashCustody senderCustody = UserCashCustody.builder()
                .id(UUID.randomUUID())
                .user(technician)
                .currentBalance(new BigDecimal("300.00"))
                .build();

        UserCashCustody receiverCustody = UserCashCustody.builder()
                .id(UUID.randomUUID())
                .user(cashier)
                .currentBalance(BigDecimal.ZERO)
                .build();

        CashTransferLog transferLog = CashTransferLog.builder()
                .id(transferLogId)
                .sender(technician)
                .receiver(cashier)
                .amount(new BigDecimal("300.00"))
                .status(CashTransferStatus.PENDING_ACCEPTANCE)
                .build();

        when(cashTransferLogRepository.findById(transferLogId)).thenReturn(Optional.of(transferLog));
        when(cashCustodyRepository.findByUserId(technicianId)).thenReturn(Optional.of(senderCustody));
        when(cashCustodyRepository.findByUserId(cashierId)).thenReturn(Optional.of(receiverCustody));

        CashTransferResponseDto result = cashCustodyService.respondTransfer(cashierId, transferLogId, true);

        assertThat(result.getStatus()).isEqualTo(CashTransferStatus.ACCEPTED);
        assertThat(senderCustody.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(receiverCustody.getCurrentBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("Apenas CFO/Administrador pode conciliar depósito bancário e liquidar o saldo do colaborador")
    void shouldAllowOnlyCfoToAuditBankDeposit() {
        UUID depositId = UUID.randomUUID();

        UserCashCustody cashierCustody = UserCashCustody.builder()
                .id(UUID.randomUUID())
                .user(cashier)
                .currentBalance(new BigDecimal("300.00"))
                .build();

        BankDepositConfirmation deposit = BankDepositConfirmation.builder()
                .id(depositId)
                .depositor(cashier)
                .amount(new BigDecimal("300.00"))
                .bankName("Banco do Brasil")
                .receiptFileUrl("https://storage.isperp.dev/receipts/dep_123.jpg")
                .status(BankDepositStatus.PENDING_AUDIT)
                .build();

        when(userRepository.findById(cfoId)).thenReturn(Optional.of(cfo));
        when(bankDepositRepository.findById(depositId)).thenReturn(Optional.of(deposit));
        when(cashCustodyRepository.findByUserId(cashierId)).thenReturn(Optional.of(cashierCustody));

        BankDepositAuditRequest auditRequest = BankDepositAuditRequest.builder()
                .approved(true)
                .notes("Conferido no extrato bancário do dia")
                .build();

        BankDepositResponseDto result = cashCustodyService.auditBankDeposit(cfoId, depositId, auditRequest);

        assertThat(result.getStatus()).isEqualTo(BankDepositStatus.CONFIRMED_IN_BANK);
        assertThat(cashierCustody.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Deve barrar tentativa de conciliação por colaborador comum sem papel financeiro")
    void shouldDenyAuditByUnauthorizedUser() {
        UUID depositId = UUID.randomUUID();
        when(userRepository.findById(technicianId)).thenReturn(Optional.of(technician));

        BankDepositAuditRequest auditRequest = BankDepositAuditRequest.builder()
                .approved(true)
                .build();

        assertThatThrownBy(() -> cashCustodyService.auditBankDeposit(technicianId, depositId, auditRequest))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");
    }
}
