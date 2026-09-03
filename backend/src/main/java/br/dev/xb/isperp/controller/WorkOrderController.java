package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.WorkOrdersApi;
import br.dev.xb.isperp.api.dto.CompleteWorkOrderRequest;
import br.dev.xb.isperp.api.dto.ScheduleWorkOrderRequest;
import br.dev.xb.isperp.api.dto.WorkOrderResponse;
import br.dev.xb.isperp.api.dto.WorkOrderStatus;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.mapper.WorkOrderMapper;
import br.dev.xb.isperp.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class WorkOrderController implements WorkOrdersApi {

    private final WorkOrderService workOrderService;
    private final WorkOrderMapper workOrderMapper;

    @Override
    public ResponseEntity<List<WorkOrderResponse>> getAllWorkOrders(WorkOrderStatus status) {
        if (status != null) {
            WorkOrder.WorkOrderStatus domainStatus = WorkOrder.WorkOrderStatus.valueOf(status.name());
            return ResponseEntity.ok(workOrderMapper.toResponseList(workOrderService.getWorkOrdersByStatus(domainStatus)));
        }
        return ResponseEntity.ok(workOrderMapper.toResponseList(workOrderService.getAllWorkOrders()));
    }

    @Override
    public ResponseEntity<WorkOrderResponse> getWorkOrderById(UUID id) {
        Optional<WorkOrder> workOrder = workOrderService.getWorkOrderById(id);
        return workOrder.map(workOrderMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<WorkOrderResponse> scheduleWorkOrder(UUID id, ScheduleWorkOrderRequest request) {
        WorkOrder scheduled = workOrderService.scheduleWorkOrder(id, workOrderMapper.toScheduleRequest(request));
        return ResponseEntity.ok(workOrderMapper.toResponse(scheduled));
    }

    @Override
    public ResponseEntity<WorkOrderResponse> completeWorkOrder(UUID id, CompleteWorkOrderRequest request) {
        WorkOrder completed = workOrderService.completeWorkOrder(id, workOrderMapper.toCompleteRequest(request));
        return ResponseEntity.ok(workOrderMapper.toResponse(completed));
    }

    @GetMapping("/work-orders/contract/{contractId}")
    public ResponseEntity<WorkOrderResponse> getWorkOrderByContractId(@PathVariable UUID contractId) {
        Optional<WorkOrder> workOrder = workOrderService.getWorkOrderByContractId(contractId);
        return workOrder.map(workOrderMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
