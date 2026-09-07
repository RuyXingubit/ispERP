package br.dev.xb.isperp.controller.financial;

import br.dev.xb.isperp.api.contract.WorkOrderFeesApi;
import br.dev.xb.isperp.api.dto.WorkOrderFeeAuditRequest;
import br.dev.xb.isperp.api.dto.WorkOrderFeeDto;
import br.dev.xb.isperp.api.dto.WorkOrderFeeWaiverRequest;
import br.dev.xb.isperp.mapper.FinancialDomainMapper;
import br.dev.xb.isperp.service.financial.WorkOrderFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkOrderFeeController implements WorkOrderFeesApi {

    private final WorkOrderFeeService workOrderFeeService;
    private final FinancialDomainMapper financialDomainMapper;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL', 'ATTENDANT')")
    public ResponseEntity<WorkOrderFeeDto> assignStandardWorkOrderFee(UUID id, Double amount) {
        BigDecimal feeAmount = amount != null ? BigDecimal.valueOf(amount) : BigDecimal.ZERO;
        var assigned = workOrderFeeService.assignStandardFee(id, feeAmount);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiWorkOrderFee(assigned));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL', 'ATTENDANT')")
    public ResponseEntity<WorkOrderFeeDto> requestWorkOrderFeeWaiver(UUID xUserId, WorkOrderFeeWaiverRequest request) {
        var domainRequest = financialDomainMapper.toDomainWorkOrderFeeWaiverRequest(request);
        var requested = workOrderFeeService.requestWaiver(xUserId, domainRequest);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiWorkOrderFee(requested));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<List<WorkOrderFeeDto>> getPendingWorkOrderFeeWaivers() {
        return ResponseEntity.ok(financialDomainMapper.toOpenApiWorkOrderFeeList(workOrderFeeService.getPendingWaiverAudits()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'CFO', 'DIRECTOR', 'FINANCIAL')")
    public ResponseEntity<WorkOrderFeeDto> auditWorkOrderFeeWaiver(UUID xUserId, UUID id, WorkOrderFeeAuditRequest request) {
        var domainRequest = financialDomainMapper.toDomainWorkOrderFeeAuditRequest(request);
        var audited = workOrderFeeService.auditWaiver(xUserId, id, domainRequest);
        return ResponseEntity.ok(financialDomainMapper.toOpenApiWorkOrderFee(audited));
    }
}
