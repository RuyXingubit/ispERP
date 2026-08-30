package br.dev.xb.isperp.repository.spec;

import br.dev.xb.isperp.entity.Invoice;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class InvoiceSpecs {

    public static Specification<Invoice> withCustomerId(@Nullable UUID customerId) {
        return (root, query, builder) -> customerId == null ? null : builder.equal(root.get("customerId"), customerId);
    }

    public static Specification<Invoice> withStatus(Invoice.@Nullable InvoiceStatus status) {
        return (root, query, builder) -> status == null ? null : builder.equal(root.get("status"), status);
    }

    public static Specification<Invoice> dueBetween(@Nullable LocalDate startDate, @Nullable LocalDate endDate) {
        return (root, query, builder) -> {
            if (startDate != null && endDate != null) {
                return builder.between(root.get("dueDate"), startDate, endDate);
            } else if (startDate != null) {
                return builder.greaterThanOrEqualTo(root.get("dueDate"), startDate);
            } else if (endDate != null) {
                return builder.lessThanOrEqualTo(root.get("dueDate"), endDate);
            }
            return null;
        };
    }

    public static Specification<Invoice> amountGreaterThanOrEqual(@Nullable BigDecimal minAmount) {
        return (root, query, builder) -> minAmount == null ? null : builder.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    public static Specification<Invoice> amountLessThanOrEqual(@Nullable BigDecimal maxAmount) {
        return (root, query, builder) -> maxAmount == null ? null : builder.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }
}
