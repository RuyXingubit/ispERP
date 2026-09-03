package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.ExpenseInstallmentDto;
import br.dev.xb.isperp.dto.financial.PayableInvoiceDto;
import br.dev.xb.isperp.dto.financial.PayableInvoiceRequest;
import br.dev.xb.isperp.entity.financial.*;
import br.dev.xb.isperp.mapper.FinancialAccountMapper;
import br.dev.xb.isperp.repository.financial.ChartOfAccountRepository;
import br.dev.xb.isperp.repository.financial.ExpenseInstallmentRepository;
import br.dev.xb.isperp.repository.financial.PayableInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayableInvoiceServiceTest {

    @Mock
    private PayableInvoiceRepository payableInvoiceRepository;

    @Mock
    private ExpenseInstallmentRepository installmentRepository;

    @Mock
    private ChartOfAccountRepository chartOfAccountRepository;

    private final FinancialAccountMapper mapper = Mappers.getMapper(FinancialAccountMapper.class);

    private PayableInvoiceService payableInvoiceService;

    @BeforeEach
    void setUp() {
        payableInvoiceService = new PayableInvoiceService(
                payableInvoiceRepository,
                installmentRepository,
                chartOfAccountRepository,
                mapper
        );
    }

    @Test
    @DisplayName("Deve gerar parcelas divididas com precisão ao cadastrar compra a pagar")
    void shouldCreatePayableWithMultipleInstallments() {
        UUID accountId = UUID.randomUUID();
        ChartOfAccount account = ChartOfAccount.builder()
                .id(accountId)
                .code("05.01.01")
                .name("Cabos Ópticos e Drop")
                .isSynthetic(false)
                .isAnalytical(true)
                .build();

        when(chartOfAccountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(payableInvoiceRepository.save(any(PayableInvoice.class))).thenAnswer(i -> {
            PayableInvoice pi = i.getArgument(0);
            pi.setId(UUID.randomUUID());
            return pi;
        });

        PayableInvoiceRequest request = PayableInvoiceRequest.builder()
                .supplierName("Fibras Brasil Ltda")
                .supplierDocument("12.345.678/0001-90")
                .chartOfAccountId(accountId)
                .description("Compra de 10 bobinas de drop 1km")
                .totalAmount(new BigDecimal("3000.00"))
                .installmentsCount(3)
                .firstDueDate(LocalDate.now().plusMonths(1))
                .build();

        PayableInvoiceDto result = payableInvoiceService.createPayableInvoice(request);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.getInstallments()).hasSize(3);
        assertThat(result.getInstallments().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getInstallments().get(1).getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.getInstallments().get(2).getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        verify(installmentRepository).saveAll(any());
    }

    @Test
    @DisplayName("Deve impedir lançamento de despesa em conta contábil sintética")
    void shouldRejectExpenseOnSyntheticAccount() {
        UUID accountId = UUID.randomUUID();
        ChartOfAccount syntheticAccount = ChartOfAccount.builder()
                .id(accountId)
                .code("05")
                .name("05. INVESTIMENTOS")
                .isSynthetic(true)
                .isAnalytical(false)
                .build();

        when(chartOfAccountRepository.findById(accountId)).thenReturn(Optional.of(syntheticAccount));

        PayableInvoiceRequest request = PayableInvoiceRequest.builder()
                .supplierName("Fornecedor")
                .chartOfAccountId(accountId)
                .description("Teste")
                .totalAmount(new BigDecimal("500.00"))
                .build();

        assertThatThrownBy(() -> payableInvoiceService.createPayableInvoice(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Não é permitido lançar despesas em contas sintéticas");
    }

    @Test
    @DisplayName("Deve quitar parcela e atualizar status da fatura para parcialmente ou totalmente paga")
    void shouldPayInstallmentAndUpdateParentStatus() {
        UUID installmentId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        PayableInvoice parent = PayableInvoice.builder()
                .id(invoiceId)
                .supplierName("Fornecedor Telecom")
                .totalAmount(new BigDecimal("2000.00"))
                .status(PayableStatus.PENDING)
                .build();

        ExpenseInstallment inst1 = ExpenseInstallment.builder()
                .id(installmentId)
                .payableInvoice(parent)
                .installmentNumber(1)
                .totalInstallments(2)
                .amount(new BigDecimal("1000.00"))
                .status(PayableStatus.PENDING)
                .build();

        ExpenseInstallment inst2 = ExpenseInstallment.builder()
                .id(UUID.randomUUID())
                .payableInvoice(parent)
                .installmentNumber(2)
                .totalInstallments(2)
                .amount(new BigDecimal("1000.00"))
                .status(PayableStatus.PENDING)
                .build();

        when(installmentRepository.findById(installmentId)).thenReturn(Optional.of(inst1));
        when(installmentRepository.save(any(ExpenseInstallment.class))).thenAnswer(i -> i.getArgument(0));
        when(installmentRepository.findByPayableInvoiceIdOrderByInstallmentNumberAsc(invoiceId)).thenReturn(List.of(inst1, inst2));

        ExpenseInstallmentDto result = payableInvoiceService.payInstallment(installmentId, new BigDecimal("1000.00"), "PIX", "https://receipt.pdf");

        assertThat(result.getStatus()).isEqualTo(PayableStatus.PAID);
        assertThat(parent.getStatus()).isEqualTo(PayableStatus.PARTIALLY_PAID);
    }
}
