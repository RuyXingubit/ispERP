package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CompleteWorkOrderRequest;
import br.dev.xb.isperp.dto.ScheduleWorkOrderRequest;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final DomainEventPublisher domainEventPublisher;

    public List<WorkOrder> getAllWorkOrders() {
        return workOrderRepository.findAll();
    }

    public List<WorkOrder> getWorkOrdersByStatus(@NonNull WorkOrder.WorkOrderStatus status) {
        return workOrderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<WorkOrder> getWorkOrderById(@NonNull UUID id) {
        return workOrderRepository.findById(id);
    }

    public Optional<WorkOrder> getWorkOrderByContractId(@NonNull UUID contractId) {
        return workOrderRepository.findByContractId(contractId);
    }

    @Transactional
    public WorkOrder createInitialInstallationWorkOrder(@NonNull UUID contractId, @NonNull UUID customerId, String notes) {
        log.info("Criando Ordem de Serviço de Instalação: contractId={}, customerId={}", contractId, customerId);

        WorkOrder workOrder = WorkOrder.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .contractId(contractId)
                .customerId(customerId)
                .type(WorkOrder.WorkOrderType.INSTALACAO)
                .status(WorkOrder.WorkOrderStatus.PENDING_SCHEDULE)
                .notes(notes)
                .build();

        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public WorkOrder scheduleWorkOrder(@NonNull UUID id, @NonNull ScheduleWorkOrderRequest request) {
        log.info("Agendando O.S. {}: data={}, período={}, técnico={}",
                id, request.getScheduledDate(), request.getScheduledPeriod(), request.getTechnicianName());

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));

        workOrder.setScheduledDate(request.getScheduledDate());
        workOrder.setScheduledPeriod(request.getScheduledPeriod());
        workOrder.setTechnicianName(request.getTechnicianName());
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            workOrder.setNotes((workOrder.getNotes() != null ? workOrder.getNotes() + "\n" : "") + request.getNotes());
        }
        workOrder.setStatus(WorkOrder.WorkOrderStatus.SCHEDULED);

        return workOrderRepository.save(workOrder);
    }

    @Transactional
    public WorkOrder completeWorkOrder(@NonNull UUID id, @NonNull CompleteWorkOrderRequest request) {
        log.info("Concluindo O.S. de campo {}: MAC={}, Serial={}, Sinal={}dBm",
                id, request.getOnuMac(), request.getOnuSerial(), request.getFiberSignalDbm());

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));

        workOrder.setOnuMac(request.getOnuMac().trim().toUpperCase());
        workOrder.setOnuSerial(request.getOnuSerial().trim().toUpperCase());
        workOrder.setFiberSignalDbm(request.getFiberSignalDbm());
        if (request.getTechnicianLatitude() != null && request.getTechnicianLongitude() != null) {
            workOrder.setTechnicianLatitude(request.getTechnicianLatitude());
            workOrder.setTechnicianLongitude(request.getTechnicianLongitude());
            workOrder.setGpsCapturedAt(LocalDateTime.now());
        }
        if (request.getInstallationPhotoUrl() != null) {
            workOrder.setInstallationPhotoUrl(request.getInstallationPhotoUrl());
        }
        workOrder.setCompletedAt(LocalDateTime.now());
        workOrder.setStatus(WorkOrder.WorkOrderStatus.COMPLETED);

        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            workOrder.setNotes((workOrder.getNotes() != null ? workOrder.getNotes() + "\n" : "") + request.getNotes());
        }

        WorkOrder saved = workOrderRepository.save(workOrder);

        // Dispara evento WORK_ORDER_COMPLETED para ativar o contrato e o faturamento
        Map<String, Object> payload = new HashMap<>();
        payload.put("workOrderId", saved.getId().toString());
        payload.put("contractId", saved.getContractId().toString());
        payload.put("customerId", saved.getCustomerId().toString());
        payload.put("onuMac", saved.getOnuMac());
        payload.put("onuSerial", saved.getOnuSerial());
        payload.put("fiberSignalDbm", saved.getFiberSignalDbm());
        if (saved.getTechnicianLatitude() != null) {
            payload.put("technicianLatitude", saved.getTechnicianLatitude().toString());
            payload.put("technicianLongitude", saved.getTechnicianLongitude().toString());
        }
        payload.put("completedAt", saved.getCompletedAt().toString());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("WORK_ORDER_COMPLETED")
                .aggregateType("WorkOrder")
                .aggregateId(saved.getId().toString())
                .payload(payload)
                .build();

        domainEventPublisher.publish(event);

        log.info("O.S. {} concluída com sucesso. Evento WORK_ORDER_COMPLETED publicado", saved.getId());
        return saved;
    }
}
