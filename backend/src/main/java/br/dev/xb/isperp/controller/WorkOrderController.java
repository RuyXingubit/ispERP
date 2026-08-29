package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.dto.CompleteWorkOrderRequest;
import br.dev.xb.isperp.dto.ScheduleWorkOrderRequest;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/work-orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @GetMapping
    public ResponseEntity<List<WorkOrder>> getAllWorkOrders(
            @RequestParam(required = false) WorkOrder.WorkOrderStatus status) {
        if (status != null) {
            return ResponseEntity.ok(workOrderService.getWorkOrdersByStatus(status));
        }
        return ResponseEntity.ok(workOrderService.getAllWorkOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getWorkOrderById(@PathVariable @NonNull UUID id) {
        Optional<WorkOrder> workOrder = workOrderService.getWorkOrderById(id);
        return workOrder.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<WorkOrder> getWorkOrderByContractId(@PathVariable @NonNull UUID contractId) {
        Optional<WorkOrder> workOrder = workOrderService.getWorkOrderByContractId(contractId);
        return workOrder.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<?> scheduleWorkOrder(
            @PathVariable @NonNull UUID id,
            @Valid @NonNull @RequestBody ScheduleWorkOrderRequest request) {
        try {
            WorkOrder scheduled = workOrderService.scheduleWorkOrder(id, request);
            return ResponseEntity.ok(scheduled);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeWorkOrder(
            @PathVariable @NonNull UUID id,
            @Valid @NonNull @RequestBody CompleteWorkOrderRequest request) {
        try {
            WorkOrder completed = workOrderService.completeWorkOrder(id, request);
            return ResponseEntity.ok(completed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
