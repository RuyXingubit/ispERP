package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.WorkOrderFeeAuditRequest;
import br.dev.xb.isperp.dto.financial.WorkOrderFeeDto;
import br.dev.xb.isperp.dto.financial.WorkOrderFeeWaiverRequest;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.entity.WorkOrder;
import br.dev.xb.isperp.entity.financial.FeeStatus;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.mapper.FinancialAccountMapper;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.WorkOrderRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderFeeService {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final br.dev.xb.isperp.repository.CustomerRepository customerRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final FinancialAccountMapper financialAccountMapper;

    @Transactional
    public WorkOrderFeeDto assignStandardFee(UUID workOrderId, BigDecimal feeAmount) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + workOrderId));

        workOrder.setStandardFeeAmount(feeAmount != null ? feeAmount : BigDecimal.ZERO);
        workOrder.setFeeStatus(feeAmount != null && feeAmount.compareTo(BigDecimal.ZERO) > 0
                ? FeeStatus.BILLABLE
                : FeeStatus.NOT_APPLICABLE);

        WorkOrder saved = workOrderRepository.save(workOrder);
        return financialAccountMapper.toDto(saved);
    }

    /**
     * O atendente solicita isenção de taxa para retenção comercial de cliente insatisfeito.
     * O atendente NÃO PODE zerar o valor por conta própria.
     */
    @Transactional
    public WorkOrderFeeDto requestWaiver(UUID attendantUserId, WorkOrderFeeWaiverRequest request) {
        WorkOrder workOrder = workOrderRepository.findById(request.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + request.getWorkOrderId()));

        if (workOrder.getStandardFeeAmount() == null || workOrder.getStandardFeeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Esta ordem de serviço não possui cobrança de taxa tabelada.");
        }

        workOrder.setFeeStatus(FeeStatus.PENDING_WAIVER_APPROVAL);
        workOrder.setWaiverReason(request.getWaiverReason());
        workOrder.setWaiverRequestedByUserId(attendantUserId);

        WorkOrder saved = workOrderRepository.save(workOrder);
        log.info("Solicitação de isenção de taxa enviada para O.S. {}. Valor: R$ {}. Motivo: {}",
                saved.getId(), saved.getStandardFeeAmount(), request.getWaiverReason());

        return financialAccountMapper.toDto(saved);
    }

    /**
     * Alçada de aprovação do Gestor Administrativo / CFO.
     * Ao aprovar a isenção, dispara notificação oficial imediata para o WhatsApp/E-mail do cliente
     * garantindo o serviço gratuito e desarmando qualquer tentativa de cobrança por fora em campo.
     */
    @Transactional
    public WorkOrderFeeDto auditWaiver(UUID managerUserId, UUID workOrderId, WorkOrderFeeAuditRequest request) {
        User manager = userRepository.findById(managerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Gestor não encontrado: " + managerUserId));

        if (manager.getRole() != UserRole.ADMIN && manager.getRole() != UserRole.CFO &&
            manager.getRole() != UserRole.DIRECTOR && manager.getRole() != UserRole.FINANCIAL) {
            throw new SecurityException("Acesso negado: Apenas CFO, Administrador ou Diretor podem aprovar isenção de taxas de serviço.");
        }

        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada: " + workOrderId));

        if (workOrder.getFeeStatus() != FeeStatus.PENDING_WAIVER_APPROVAL) {
            throw new IllegalStateException("Esta O.S. não possui solicitação de isenção pendente de aprovação.");
        }

        workOrder.setWaiverAuditedByUserId(managerUserId);
        workOrder.setWaiverAuditedAt(OffsetDateTime.now());

        String protocol = "OS-" + workOrder.getId().toString().substring(0, 8).toUpperCase();

        if (Boolean.TRUE.equals(request.getApproved())) {
            workOrder.setFeeStatus(FeeStatus.WAIVED_APPROVED);
            log.info("Isenção de taxa APROVADA pelo gestor {} para O.S. {}. Valor isento: R$ {}",
                    manager.getName(), protocol, workOrder.getStandardFeeAmount());

            // Disparo do evento de domínio para envio de WhatsApp/E-mail oficial anti-fraude ao cliente
            Customer customer = customerRepository.findById(workOrder.getCustomerId()).orElse(null);

            Map<String, Object> payload = new HashMap<>();
            payload.put("workOrderId", workOrder.getId());
            payload.put("protocol", protocol);
            payload.put("customerId", workOrder.getCustomerId());
            payload.put("customerName", customer != null ? customer.getName() : "");
            payload.put("customerPhone", customer != null ? customer.getPhone() : "");
            payload.put("serviceType", workOrder.getType() != null ? workOrder.getType().name() : "");
            payload.put("waivedAmount", workOrder.getStandardFeeAmount());
            payload.put("auditedByManagerName", manager.getName());

            domainEventPublisher.publish(GenericDomainEvent.builder()
                    .eventType("WORK_ORDER_FEE_WAIVED")
                    .aggregateType("WorkOrder")
                    .aggregateId(workOrder.getId().toString())
                    .payload(payload)
                    .build());
        } else {
            workOrder.setFeeStatus(FeeStatus.WAIVED_REJECTED);
            log.warn("Isenção de taxa REJEITADA pelo gestor {} para O.S. {}. O valor deve ser faturado normalmente.",
                    manager.getName(), protocol);
        }

        WorkOrder saved = workOrderRepository.save(workOrder);
        return financialAccountMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderFeeDto> getPendingWaiverAudits() {
        List<WorkOrder> list = workOrderRepository.findAll().stream()
                .filter(wo -> wo.getFeeStatus() == FeeStatus.PENDING_WAIVER_APPROVAL)
                .toList();
        return financialAccountMapper.toWorkOrderFeeDtoList(list);
    }
}
