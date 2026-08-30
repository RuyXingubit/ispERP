package br.dev.xb.isperp.repository.spec;

import br.dev.xb.isperp.entity.WorkOrder;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class WorkOrderSpecs {

    public static Specification<WorkOrder> withStatus(WorkOrder.@Nullable WorkOrderStatus status) {
        return (root, query, builder) -> status == null ? null : builder.equal(root.get("status"), status);
    }

    public static Specification<WorkOrder> withType(WorkOrder.@Nullable WorkOrderType type) {
        return (root, query, builder) -> type == null ? null : builder.equal(root.get("type"), type);
    }

    public static Specification<WorkOrder> withTechnicianName(@Nullable String technicianName) {
        return (root, query, builder) -> technicianName == null || technicianName.isBlank()
                ? null
                : builder.like(builder.lower(root.get("technicianName")), "%" + technicianName.toLowerCase() + "%");
    }

    public static Specification<WorkOrder> withCustomerId(@Nullable UUID customerId) {
        return (root, query, builder) -> customerId == null ? null : builder.equal(root.get("customerId"), customerId);
    }
}
