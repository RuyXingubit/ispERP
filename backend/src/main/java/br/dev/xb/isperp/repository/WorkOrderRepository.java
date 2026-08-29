package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {

    List<WorkOrder> findByStatusOrderByCreatedAtDesc(WorkOrder.WorkOrderStatus status);

    List<WorkOrder> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    Optional<WorkOrder> findByContractId(UUID contractId);

    List<WorkOrder> findByTechnicianNameOrderByScheduledDateAsc(String technicianName);

    long countByStatus(WorkOrder.WorkOrderStatus status);
}
