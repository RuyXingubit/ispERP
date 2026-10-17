package br.dev.xb.isperp.service;

import br.dev.xb.isperp.api.dto.ScheduleAvailabilityResponse;
import br.dev.xb.isperp.api.dto.ShiftPeriodSummaryDto;
import br.dev.xb.isperp.api.dto.TechnicianShiftSlotDto;
import br.dev.xb.isperp.dto.CompleteWorkOrderRequest;
import br.dev.xb.isperp.dto.ScheduleWorkOrderRequest;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class WorkOrderService {

    public static final int MAX_WORK_ORDERS_PER_TECHNICIAN_SHIFT = 4;
    private static final List<String> STANDARD_SHIFTS = List.of("MANHA", "TARDE");

    private final WorkOrderRepository workOrderRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Autowired(required = false)
    private @Nullable UserRepository userRepository;

    public List<WorkOrder> getAllWorkOrders() {
        return workOrderRepository.findAll();
    }

    public List<WorkOrder> getWorkOrdersByStatus(WorkOrder.WorkOrderStatus status) {
        return workOrderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public Optional<WorkOrder> getWorkOrderById(UUID id) {
        return workOrderRepository.findById(id);
    }

    public Optional<WorkOrder> getWorkOrderByContractId(UUID contractId) {
        return workOrderRepository.findByContractId(contractId);
    }

    @Transactional
    public WorkOrder createInitialInstallationWorkOrder(UUID contractId, @Nullable UUID customerId, @Nullable String notes) {
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
    public WorkOrder scheduleWorkOrder(UUID id, ScheduleWorkOrderRequest request) {
        log.info("Agendando O.S. {}: data={}, período={}, técnico={}",
                id, request.getScheduledDate(), request.getScheduledPeriod(), request.getTechnicianName());

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ordem de serviço não encontrada"));

        String normalizedPeriod = request.getScheduledPeriod() != null
                ? request.getScheduledPeriod().trim().toUpperCase()
                : "MANHA";
        String techName = request.getTechnicianName() != null ? request.getTechnicianName().trim() : "";

        if (request.getScheduledDate() != null && !techName.isEmpty() && STANDARD_SHIFTS.contains(normalizedPeriod)) {
            List<WorkOrder> existingInShift = workOrderRepository
                    .findByScheduledDateAndScheduledPeriodIgnoreCaseAndTechnicianNameIgnoreCase(
                            request.getScheduledDate(),
                            normalizedPeriod,
                            techName
                    );
            long activeCount = existingInShift != null ? existingInShift.stream()
                    .filter(wo -> !wo.getId().equals(id))
                    .filter(wo -> wo.getStatus() == WorkOrder.WorkOrderStatus.SCHEDULED
                            || wo.getStatus() == WorkOrder.WorkOrderStatus.IN_PROGRESS)
                    .count() : 0;

            if (activeCount >= MAX_WORK_ORDERS_PER_TECHNICIAN_SHIFT) {
                throw new IllegalStateException(String.format(
                        "Capacidade máxima atingida para o técnico %s no turno %s em %s (%d/%d O.S.).",
                        techName, normalizedPeriod, request.getScheduledDate(), activeCount, MAX_WORK_ORDERS_PER_TECHNICIAN_SHIFT
                ));
            }
        }

        workOrder.setScheduledDate(request.getScheduledDate());
        workOrder.setScheduledPeriod(normalizedPeriod);
        workOrder.setTechnicianName(techName);
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            workOrder.setNotes((workOrder.getNotes() != null ? workOrder.getNotes() + "\n" : "") + request.getNotes());
        }
        workOrder.setStatus(WorkOrder.WorkOrderStatus.SCHEDULED);

        return workOrderRepository.save(workOrder);
    }

    @Transactional(readOnly = true)
    public ScheduleAvailabilityResponse getScheduleAvailability(@Nullable LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<User> technicians = new ArrayList<>();
        if (userRepository != null) {
            technicians = userRepository.findByRole(UserRole.TECHNICIAN);
            if (technicians.isEmpty()) {
                technicians = userRepository.findAll();
            }
        }
        if (technicians.isEmpty()) {
            technicians = List.of(
                    User.builder().id(UuidCreatorUtils.generateUuidV7()).name("Equipe Técnica 01").role(UserRole.TECHNICIAN).build(),
                    User.builder().id(UuidCreatorUtils.generateUuidV7()).name("Equipe Técnica 02").role(UserRole.TECHNICIAN).build()
            );
        }

        List<WorkOrder> dayOrders = workOrderRepository.findByScheduledDate(targetDate);
        if (dayOrders == null) {
            dayOrders = List.of();
        }

        List<ShiftPeriodSummaryDto> shifts = new ArrayList<>();
        for (String shift : STANDARD_SHIFTS) {
            List<TechnicianShiftSlotDto> techSlots = new ArrayList<>();
            int totalCapacity = 0;
            int totalOccupied = 0;

            for (User tech : technicians) {
                long occupied = dayOrders.stream()
                        .filter(wo -> wo.getStatus() == WorkOrder.WorkOrderStatus.SCHEDULED
                                || wo.getStatus() == WorkOrder.WorkOrderStatus.IN_PROGRESS)
                        .filter(wo -> shift.equalsIgnoreCase(wo.getScheduledPeriod()))
                        .filter(wo -> tech.getName() != null && tech.getName().equalsIgnoreCase(wo.getTechnicianName()))
                        .count();

                int occInt = (int) occupied;
                int availInt = Math.max(0, MAX_WORK_ORDERS_PER_TECHNICIAN_SHIFT - occInt);

                TechnicianShiftSlotDto slotDto = new TechnicianShiftSlotDto();
                slotDto.setTechnicianId(tech.getId());
                slotDto.setTechnicianName(tech.getName());
                slotDto.setPeriod(shift);
                slotDto.setMaxCapacity(MAX_WORK_ORDERS_PER_TECHNICIAN_SHIFT);
                slotDto.setOccupiedSlots(occInt);
                slotDto.setAvailableSlots(availInt);
                slotDto.setAvailable(availInt > 0);
                techSlots.add(slotDto);

                totalCapacity += MAX_WORK_ORDERS_PER_TECHNICIAN_SHIFT;
                totalOccupied += occInt;
            }

            int totalAvailable = Math.max(0, totalCapacity - totalOccupied);
            ShiftPeriodSummaryDto summaryDto = new ShiftPeriodSummaryDto();
            summaryDto.setPeriod(shift);
            summaryDto.setTotalCapacity(totalCapacity);
            summaryDto.setOccupiedSlots(totalOccupied);
            summaryDto.setAvailableSlots(totalAvailable);
            summaryDto.setAvailable(totalAvailable > 0);
            summaryDto.setTechnicians(techSlots);
            shifts.add(summaryDto);
        }

        ScheduleAvailabilityResponse response = new ScheduleAvailabilityResponse();
        response.setDate(targetDate);
        response.setMaxCapacityPerTechnicianPerShift(MAX_WORK_ORDERS_PER_TECHNICIAN_SHIFT);
        response.setShifts(shifts);
        return response;
    }

    public void setUserRepository(@Nullable UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public WorkOrder completeWorkOrder(UUID id, CompleteWorkOrderRequest request) {
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
        if (request.getDigitalSignatureBase64() != null) {
            workOrder.setDigitalSignatureBase64(request.getDigitalSignatureBase64());
        }
        if (request.getCustomerSignatureName() != null) {
            workOrder.setCustomerSignatureName(request.getCustomerSignatureName());
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
