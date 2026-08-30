package br.dev.xb.isperp.repository.spec;

import br.dev.xb.isperp.entity.Invoice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceSpecsTest {

    @Test
    @DisplayName("Deve construir specifications para filtros de faturas sem erros de nulo")
    void shouldBuildInvoiceSpecifications() {
        UUID customerId = UUID.randomUUID();
        LocalDate now = LocalDate.now();

        Specification<Invoice> specCustomer = InvoiceSpecs.withCustomerId(customerId);
        assertThat(specCustomer).isNotNull();

        Specification<Invoice> specNullCustomer = InvoiceSpecs.withCustomerId(null);
        assertThat(specNullCustomer).isNotNull();

        Specification<Invoice> specStatus = InvoiceSpecs.withStatus(Invoice.InvoiceStatus.OVERDUE);
        assertThat(specStatus).isNotNull();

        Specification<Invoice> specDates = InvoiceSpecs.dueBetween(now.minusDays(30), now);
        assertThat(specDates).isNotNull();

        Specification<Invoice> specMinAmount = InvoiceSpecs.amountGreaterThanOrEqual(new BigDecimal("50.00"));
        assertThat(specMinAmount).isNotNull();

        Specification<Invoice> specMaxAmount = InvoiceSpecs.amountLessThanOrEqual(new BigDecimal("500.00"));
        assertThat(specMaxAmount).isNotNull();
    }
}
